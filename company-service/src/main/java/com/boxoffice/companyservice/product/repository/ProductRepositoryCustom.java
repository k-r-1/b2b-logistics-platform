package com.boxoffice.companyservice.product.repository;

import com.boxoffice.companyservice.product.dto.search.ProductSearchCondition;
import com.boxoffice.companyservice.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryCustom {

    Optional<Product> findProduct(UUID companyId, UUID productId);

    Page<Product> searchProducts(UUID companyId, ProductSearchCondition condition, Pageable pageable);

    Page<Product> searchProducts(ProductSearchCondition condition, Pageable pageable);
}
