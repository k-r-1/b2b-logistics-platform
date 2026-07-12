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
import com.boxoffice.companyservice.product.exception.ProductErrorCode;
import com.boxoffice.companyservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final CompanyService companyService;
    private final ProductStockRedisManager stockRedisManager;

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

        List<Product> products = productRepository.findAllById(productIds);
        validateAllProductsFound(products, quantityByProductId);
        validateProductsBelongToSupplier(products, supplier.getId());

        // Redis 키가 없을 때 시드할 현재 DB 재고 (productIds 순서에 맞춤)
        Map<UUID, Product> productById = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<Integer> quantities = productIds.stream().map(quantityByProductId::get).toList();
        List<Integer> dbStocks = productIds.stream().map(id -> productById.get(id).getStockQuantity()).toList();

        // 확인+차감+멱등성을 Redis Lua가 원자적으로 처리 (락 불필요)
        ProductStockRedisManager.DeductResult result =
                stockRedisManager.deduct(orderId, productIds, quantities, dbStocks);

        if (result.status() == ProductStockRedisManager.DeductStatus.INSUFFICIENT) {
            throw new BaseException(ProductErrorCode.INSUFFICIENT_STOCK);
        }
        // 새 차감이면 DB에도 락 없이 원자 UPDATE로 반영 (중복 주문이면 재고 변화 없음)
        if (result.status() == ProductStockRedisManager.DeductStatus.SUCCESS) {
            quantityByProductId.forEach(productRepository::decreaseStock);
        }
        return ProductStockDeductResponseDto.from(products, quantityByProductId, supplier.getHubId(), receiver.getHubId());
    }

    @Transactional
    public void restoreStocks(UUID orderId, List<ProductStockItemRequestDto> request) {
        Map<UUID, Integer> quantityByProductId = toQuantityMap(request);
        List<UUID> productIds = toSortedProductIds(quantityByProductId);
        List<Integer> quantities = productIds.stream().map(quantityByProductId::get).toList();

        ProductStockRedisManager.RestoreStatus status =
                stockRedisManager.restore(orderId, productIds, quantities);

        if (status == ProductStockRedisManager.RestoreStatus.NO_DEDUCT_HISTORY) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
        // 새 복원이면 DB에도 반영 (중복 복원이면 변화 없음)
        if (status == ProductStockRedisManager.RestoreStatus.SUCCESS) {
            quantityByProductId.forEach(productRepository::increaseStock);
        }
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

    private void validateAllProductsFound(List<Product> products, Map<UUID, Integer> quantityByProductId) {
        if (products.size() != quantityByProductId.size()) {
            throw new BaseException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    private void validateProductsBelongToSupplier(List<Product> products, UUID supplierId) {
        boolean hasOtherCompanyProduct = products.stream()
                .anyMatch(product -> !product.getCompany().getId().equals(supplierId));

        if (hasOtherCompanyProduct) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }
}
