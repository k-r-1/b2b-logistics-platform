package com.boxoffice.companyservice.product.service;

import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.exception.CompanyErrorCode;
import com.boxoffice.companyservice.company.repository.CompanyRepository;
import com.boxoffice.companyservice.product.domain.PriceVO;
import com.boxoffice.companyservice.product.dto.request.ProductCreateRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductStockDeductRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductStockItemRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductStockRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductStockRestoreRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductUpdateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import com.boxoffice.companyservice.product.dto.response.HubStockCountResponseDto;
import com.boxoffice.companyservice.product.dto.response.ProductResponseDto;
import com.boxoffice.companyservice.product.dto.search.ProductSearchCondition;
import com.boxoffice.companyservice.product.entity.Product;
import com.boxoffice.companyservice.product.entity.ProductStockOperationType;
import com.boxoffice.companyservice.product.exception.ProductErrorCode;
import com.boxoffice.companyservice.product.repository.ProductRepository;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private static final Duration STOCK_OPERATION_LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration STOCK_OPERATION_DONE_TTL = Duration.ofDays(7);

    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
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
    public void checkStocks(ProductStockRequestDto request) {
        Map<UUID, Integer> quantityByProductId = toQuantityMap(request.getItems());
        List<UUID> productIds = toSortedProductIds(quantityByProductId);
        // check는 재고 예약이 아니므로 최종 주문 성공 여부는 deduct 결과로 판단해야 한다.
        List<Product> products = productRepository.findAllById(productIds);

        validateAllProductsFound(products, quantityByProductId);
        validateEnoughStock(products, quantityByProductId);
    }

    @Transactional
    public void deductStocks(ProductStockDeductRequestDto request) {
        if (isStockOperationDone(request.getOrderId(), ProductStockOperationType.DEDUCT)) {
            return;
        }
        acquireStockOperationLock(request.getOrderId(), ProductStockOperationType.DEDUCT);

        Map<UUID, Integer> quantityByProductId = toQuantityMap(request.getItems());
        List<UUID> productIds = toSortedProductIds(quantityByProductId);
        List<Product> products = productRepository.findAllByIdInForUpdate(productIds);

        validateAllProductsFound(products, quantityByProductId);
        validateEnoughStock(products, quantityByProductId);
        products.forEach(product -> product.deductStock(quantityByProductId.get(product.getId())));
        markStockOperationDoneAfterCommit(request.getOrderId(), ProductStockOperationType.DEDUCT);
    }

    @Transactional
    public void restoreStocks(ProductStockRestoreRequestDto request) {
        if (!isStockOperationDone(request.getOrderId(), ProductStockOperationType.DEDUCT)) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }

        if (isStockOperationDone(request.getOrderId(), ProductStockOperationType.RESTORE)) {
            return;
        }
        acquireStockOperationLock(request.getOrderId(), ProductStockOperationType.RESTORE);

        Map<UUID, Integer> quantityByProductId = toQuantityMap(request.getItems());
        List<UUID> productIds = toSortedProductIds(quantityByProductId);
        List<Product> products = productRepository.findAllByIdInForUpdate(productIds);

        validateAllProductsFound(products, quantityByProductId);
        products.forEach(product -> product.restoreStock(quantityByProductId.get(product.getId())));
        markStockOperationDoneAfterCommit(request.getOrderId(), ProductStockOperationType.RESTORE);
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
        return Boolean.TRUE.equals(redisTemplate.hasKey(doneKey(orderId, operationType)));
    }

    private void acquireStockOperationLock(UUID orderId, ProductStockOperationType operationType) {
        // Redis SET NX + TTL로 같은 orderId의 중복 재고 작업만 짧게 막는다.
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey(orderId, operationType), "1", STOCK_OPERATION_LOCK_TTL);

        if (!Boolean.TRUE.equals(locked)) {
            throw new BaseException(ProductErrorCode.STOCK_OPERATION_IN_PROGRESS);
        }

        deleteLockAfterCompletion(orderId, operationType);
    }

    private void markStockOperationDoneAfterCommit(UUID orderId, ProductStockOperationType operationType) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            redisTemplate.opsForValue().set(doneKey(orderId, operationType), "1", STOCK_OPERATION_DONE_TTL);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisTemplate.opsForValue().set(doneKey(orderId, operationType), "1", STOCK_OPERATION_DONE_TTL);
            }
        });
    }

    private void deleteLockAfterCompletion(UUID orderId, ProductStockOperationType operationType) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            redisTemplate.delete(lockKey(orderId, operationType));
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                redisTemplate.delete(lockKey(orderId, operationType));
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
}
