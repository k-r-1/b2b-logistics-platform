package com.boxoffice.companyservice.product.service;

import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.exception.CompanyErrorCode;
import com.boxoffice.companyservice.company.repository.CompanyRepository;
import com.boxoffice.companyservice.product.domain.PriceVO;
import com.boxoffice.companyservice.product.dto.request.ProductCreateRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductUpdateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import com.boxoffice.companyservice.product.dto.response.ProductResponseDto;
import com.boxoffice.companyservice.product.dto.search.ProductSearchCondition;
import com.boxoffice.companyservice.product.entity.Product;
import com.boxoffice.companyservice.product.exception.ProductErrorCode;
import com.boxoffice.companyservice.product.repository.ProductRepository;
import com.boxoffice.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;

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
}
