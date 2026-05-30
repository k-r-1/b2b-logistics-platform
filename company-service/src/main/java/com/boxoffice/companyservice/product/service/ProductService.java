package com.boxoffice.companyservice.product.service;

import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.exception.CompanyErrorCode;
import com.boxoffice.companyservice.company.repository.CompanyRepository;
import com.boxoffice.companyservice.product.domain.PriceVO;
import com.boxoffice.companyservice.product.dto.request.ProductCreateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import com.boxoffice.companyservice.product.entity.Product;
import com.boxoffice.companyservice.product.repository.ProductRepository;
import com.boxoffice.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
