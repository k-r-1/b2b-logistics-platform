package com.boxoffice.companyservice.product.repository;

import com.boxoffice.companyservice.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, ProductRepositoryCustom {

    // 동시성/재고 판단은 Redis Lua가 끝내므로, DB에는 락 없이 원자적으로 반영만 한다.
    @Modifying(clearAutomatically = true)
    @Query("update Product p set p.stockQuantity = p.stockQuantity - :quantity where p.id = :id")
    void decreaseStock(@Param("id") UUID id, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true)
    @Query("update Product p set p.stockQuantity = p.stockQuantity + :quantity where p.id = :id")
    void increaseStock(@Param("id") UUID id, @Param("quantity") int quantity);
}
