package com.boxoffice.companyservice.product.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.exception.CompanyErrorCode;
import com.boxoffice.companyservice.company.repository.CompanyRepository;
import com.boxoffice.companyservice.company.service.CompanyService;
import com.boxoffice.companyservice.product.domain.PriceVO;
import com.boxoffice.companyservice.product.dto.request.ProductCreateRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductStockDeductRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductStockItemRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductUpdateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import com.boxoffice.companyservice.product.dto.response.HubStockCountResponseDto;
import com.boxoffice.companyservice.product.dto.response.ProductStockCheckResponseDto;
import com.boxoffice.companyservice.product.dto.response.ProductStockDeductResponseDto;
import com.boxoffice.companyservice.product.dto.response.ProductResponseDto;
import com.boxoffice.companyservice.product.dto.search.ProductSearchCondition;
import com.boxoffice.companyservice.product.entity.Product;
import com.boxoffice.companyservice.product.entity.ProductStockOperationType;
import com.boxoffice.companyservice.product.exception.ProductErrorCode;
import com.boxoffice.companyservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private static final String STOCK_OPERATION_KEY_PREFIX = "company-service:product-stock";
    private static final Duration STOCK_OPERATION_LOCK_TTL = Duration.ofMinutes(1);
    private static final Duration STOCK_OPERATION_DONE_TTL = Duration.ofDays(7);

    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final CompanyService companyService;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public ProductCreateResponseDto createProduct(UUID companyId, ProductCreateRequestDto request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BaseException(CompanyErrorCode.COMPANY_NOT_FOUND));
        Product product = Product.create(
                request.getName(),
                PriceVO.create(request.getPrice()),
                request.getStockQuantity(),
                company
        );

        Product savedProduct = productRepository.save(product);
        log.info("Product created. productId={}, companyId={}, price={}, stockQuantity={}",
                savedProduct.getId(), company.getId(), savedProduct.getPrice().getValue(), savedProduct.getStockQuantity());

        return ProductCreateResponseDto.from(savedProduct);
    }

    @Transactional
    public void updateProduct(UUID companyId, UUID productId, ProductUpdateRequestDto request) {
        Product product = productRepository.findProduct(companyId, productId)
                .orElseThrow(() -> new BaseException(ProductErrorCode.PRODUCT_NOT_FOUND));
        PriceVO price = request.getPrice() == null ? null : PriceVO.create(request.getPrice());

        product.update(request.getName(), price, request.getStockQuantity());
        log.info("Product updated. productId={}, companyId={}, name={}, price={}, stockQuantity={}",
                product.getId(), product.getCompany().getId(), product.getName(),
                product.getPrice().getValue(), product.getStockQuantity());
    }

    @Transactional
    public void deleteProduct(UUID companyId, UUID productId, UUID deletedBy) {
        Product product = productRepository.findProduct(companyId, productId)
                .orElseThrow(() -> new BaseException(ProductErrorCode.PRODUCT_NOT_FOUND));

        product.softDelete(deletedBy);
        log.info("Product deleted. productId={}, companyId={}, deletedBy={}",
                product.getId(), product.getCompany().getId(), deletedBy);
    }

    @Transactional(readOnly = true)
    public List<HubStockCountResponseDto> getHubStockCounts(List<UUID> hubIds) {
        Map<UUID, Long> stockCountByHubId = productRepository.sumStockQuantityByHubIds(hubIds);

        return hubIds.stream()
                .map(hubId -> new HubStockCountResponseDto(hubId, stockCountByHubId.getOrDefault(hubId, 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, Long> getCompanyStockCountMap(List<UUID> companyIds) {
        return productRepository.sumStockQuantityByCompanyIds(companyIds);
    }

    @Transactional(readOnly = true)
    public ProductStockCheckResponseDto checkStocks(List<ProductStockItemRequestDto> request) {
        Map<UUID, Integer> quantityByProductId = toQuantityMap(request);
        List<UUID> productIds = toSortedProductIds(quantityByProductId);
        // check는 재고 예약이 아니므로 최종 주문 성공 여부는 deduct 결과로 판단해야 한다.
        List<Product> products = productRepository.findAllById(productIds);

        validateAllProductsFound(products, quantityByProductId);
        return ProductStockCheckResponseDto.from(products, quantityByProductId);
    }

    @Transactional
    public ProductStockDeductResponseDto deductStocks(UUID orderId, ProductStockDeductRequestDto request) {
        Map<UUID, Integer> quantityByProductId = toQuantityMap(request.getProducts());
        List<UUID> productIds = toSortedProductIds(quantityByProductId);

        Company supplier = companyService.getCompanyEntity(request.getSupplierId());
        Company receiver = companyService.getCompanyEntity(request.getReceiverId());

        if (isStockOperationDone(orderId, ProductStockOperationType.DEDUCT)) {
            log.info("Stock operation already done before lock. orderId={}, operationType={}", orderId, ProductStockOperationType.DEDUCT);
            return getDeductResponseWithoutStockChange(productIds, quantityByProductId, supplier, receiver);
        }
        acquireStockOperationLock(orderId, ProductStockOperationType.DEDUCT);
        if (isStockOperationDone(orderId, ProductStockOperationType.DEDUCT)) {
            log.info("Stock operation already done after lock. orderId={}, operationType={}", orderId, ProductStockOperationType.DEDUCT);
            return getDeductResponseWithoutStockChange(productIds, quantityByProductId, supplier, receiver);
        }

        List<Product> products = productRepository.findAllByIdInForUpdate(productIds);

        validateAllProductsFound(products, quantityByProductId);
        validateProductsBelongToSupplier(products, supplier.getId());
        validateEnoughStock(products, quantityByProductId);
        products.forEach(product -> product.deductStock(quantityByProductId.get(product.getId())));
        markStockOperationDoneAfterCommit(orderId, ProductStockOperationType.DEDUCT);
        return ProductStockDeductResponseDto.from(products, quantityByProductId, supplier.getHubId(), receiver.getHubId());
    }

    @Transactional
    public void restoreStocks(UUID orderId, List<ProductStockItemRequestDto> request) {
        if (!isStockOperationDone(orderId, ProductStockOperationType.DEDUCT)) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }

        if (isStockOperationDone(orderId, ProductStockOperationType.RESTORE)) {
            log.info("Stock operation already done before lock. orderId={}, operationType={}", orderId, ProductStockOperationType.RESTORE);
            return;
        }
        acquireStockOperationLock(orderId, ProductStockOperationType.RESTORE);
        if (isStockOperationDone(orderId, ProductStockOperationType.RESTORE)) {
            log.info("Stock operation already done after lock. orderId={}, operationType={}", orderId, ProductStockOperationType.RESTORE);
            return;
        }

        Map<UUID, Integer> quantityByProductId = toQuantityMap(request);
        List<UUID> productIds = toSortedProductIds(quantityByProductId);
        List<Product> products = productRepository.findAllByIdInForUpdate(productIds);

        validateAllProductsFound(products, quantityByProductId);
        products.forEach(product -> product.restoreStock(quantityByProductId.get(product.getId())));
        markStockOperationDoneAfterCommit(orderId, ProductStockOperationType.RESTORE);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProduct(UUID companyId, UUID productId) {
        Product product = productRepository.findProduct(companyId, productId)
                .orElseThrow(() -> new BaseException(ProductErrorCode.PRODUCT_NOT_FOUND));

        return ProductResponseDto.from(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> searchProducts(
            UUID companyId,
            ProductSearchCondition condition,
            Pageable pageable
    ) {
        return productRepository.searchProducts(companyId, condition, pageable)
                .map(ProductResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> searchProducts(ProductSearchCondition condition, Pageable pageable) {
        return productRepository.searchProducts(condition, pageable)
                .map(ProductResponseDto::from);
    }

    private Map<UUID, Integer> toQuantityMap(List<ProductStockItemRequestDto> items) {
        if (items == null || items.isEmpty()) {
            throw new BaseException(ProductErrorCode.INVALID_STOCK_QUANTITY);
        }

        Map<UUID, Integer> quantityByProductId = new LinkedHashMap<>();
        for (ProductStockItemRequestDto item : items) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BaseException(ProductErrorCode.INVALID_STOCK_QUANTITY);
            }
            // 요청 리스트에 같은 상품이 여러 번 쪼개져 있어도 총 차감 수량으로 병합합니다.
            quantityByProductId.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantityByProductId;
    }

    private List<UUID> toSortedProductIds(Map<UUID, Integer> quantityByProductId) {
        return quantityByProductId.keySet().stream()
                .sorted()
                .toList();
    }

    private boolean isStockOperationDone(UUID orderId, ProductStockOperationType operationType) {
        String key = doneKey(orderId, operationType);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private ProductStockDeductResponseDto getDeductResponseWithoutStockChange(
            List<UUID> productIds,
            Map<UUID, Integer> quantityByProductId,
            Company supplier,
            Company receiver
    ) {
        List<Product> products = productRepository.findAllById(productIds);
        validateAllProductsFound(products, quantityByProductId);
        validateProductsBelongToSupplier(products, supplier.getId());
        return ProductStockDeductResponseDto.from(products, quantityByProductId, supplier.getHubId(), receiver.getHubId());
    }

    private void acquireStockOperationLock(UUID orderId, ProductStockOperationType operationType) {
        String key = lockKey(orderId, operationType);
        // Redis SET NX + TTL로 같은 orderId의 중복 재고 작업만 짧게 막는다.
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", STOCK_OPERATION_LOCK_TTL);

        if (!Boolean.TRUE.equals(locked)) {
            log.warn("Stock operation already in progress. orderId={}, operationType={}", orderId, operationType);
            throw new BaseException(ProductErrorCode.STOCK_OPERATION_IN_PROGRESS);
        }

        deleteLockAfterCompletion(orderId, operationType);
    }

    private void markStockOperationDoneAfterCommit(UUID orderId, ProductStockOperationType operationType) {
        String key = doneKey(orderId, operationType);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            saveStockOperationDoneKey(orderId, operationType, key);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                saveStockOperationDoneKey(orderId, operationType, key);
            }
        });
    }

    private void saveStockOperationDoneKey(UUID orderId, ProductStockOperationType operationType, String key) {
        try {
            redisTemplate.opsForValue().set(key, "1", STOCK_OPERATION_DONE_TTL);
        } catch (RuntimeException e) {
            log.error("Failed to save stock operation done key. orderId={}, operationType={}, key={}",
                    orderId, operationType, key, e);
        }
    }

    private void deleteLockAfterCompletion(UUID orderId, ProductStockOperationType operationType) {
        String key = lockKey(orderId, operationType);
        // 트랜잭션 외부에서 호출된 경우 즉시 락 해제 (방어 코드)
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            redisTemplate.delete(key);
            return;
        }

        // 트랜잭션 내부인 경우, Commit/Rollback 완료 후 락 해제 보장
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                redisTemplate.delete(key);
            }
        });
    }

    private String doneKey(UUID orderId, ProductStockOperationType operationType) {
        return STOCK_OPERATION_KEY_PREFIX + ":done:" + operationType.name().toLowerCase() + ":" + orderId;
    }

    private String lockKey(UUID orderId, ProductStockOperationType operationType) {
        return STOCK_OPERATION_KEY_PREFIX + ":lock:" + operationType.name().toLowerCase() + ":" + orderId;
    }

    private void validateAllProductsFound(List<Product> products, Map<UUID, Integer> quantityByProductId) {
        if (products.size() != quantityByProductId.size()) {
            throw new BaseException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    private void validateEnoughStock(List<Product> products, Map<UUID, Integer> quantityByProductId) {
        Map<UUID, Product> productById = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        quantityByProductId.forEach((productId, quantity) -> {
            Product product = productById.get(productId);
            if (product == null) {
                throw new BaseException(ProductErrorCode.PRODUCT_NOT_FOUND);
            }
            if (product.getStockQuantity() < quantity) {
                throw new BaseException(ProductErrorCode.INSUFFICIENT_STOCK);
            }
        });
    }

    private void validateProductsBelongToSupplier(List<Product> products, UUID supplierId) {
        boolean hasOtherCompanyProduct = products.stream()
                .anyMatch(product -> !product.getCompany().getId().equals(supplierId));

        if (hasOtherCompanyProduct) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }
}
