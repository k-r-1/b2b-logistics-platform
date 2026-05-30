package com.boxoffice.companyservice.product.repository;

import com.boxoffice.companyservice.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, ProductRepositoryCustom {
}
