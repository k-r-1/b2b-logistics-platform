package com.boxoffice.companyservice.product.entity;

import com.boxoffice.common.entity.BaseEntity;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.product.domain.PriceVO;
import com.boxoffice.companyservice.product.exception.ProductErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "p_products")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Embedded
    private PriceVO price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    private Product(String name, PriceVO price, Integer stockQuantity, Company company) {
        validateName(name);
        validatePrice(price);
        validateStockQuantity(stockQuantity);
        validateCompany(company);

        this.name = name.trim();
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.company = company;
    }

    public static Product create(String name, PriceVO price, Integer stockQuantity, Company company) {
        return new Product(name, price, stockQuantity, company);
    }

    public void update(String name, PriceVO price, Integer stockQuantity) {
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (price != null) {
            validatePrice(price);
            this.price = price;
        }
        if (stockQuantity != null) {
            validateStockQuantity(stockQuantity);
            this.stockQuantity = stockQuantity;
        }
    }

    public void deductStock(int quantity) {
        validateOrderQuantity(quantity);
        if (this.stockQuantity < quantity) {
            throw new BaseException(ProductErrorCode.INSUFFICIENT_STOCK);
        }
        this.stockQuantity -= quantity;
    }

    public void restoreStock(int quantity) {
        validateOrderQuantity(quantity);
        this.stockQuantity += quantity;
    }

    private static void validateName(String name) {
        // 도메인 검증 실패는 공통 예외 응답 정책에 맞춰 BaseException으로 변환한다.
        if (name == null || name.isBlank()) {
            throw new BaseException(ProductErrorCode.INVALID_PRODUCT_NAME);
        }
    }

    private static void validatePrice(PriceVO price) {
        if (price == null) {
            throw new BaseException(ProductErrorCode.INVALID_PRODUCT_PRICE);
        }
    }

    private static void validateStockQuantity(Integer stockQuantity) {
        if (stockQuantity == null) {
            throw new BaseException(ProductErrorCode.INVALID_STOCK_QUANTITY);
        }

        if (stockQuantity < 0) {
            throw new BaseException(ProductErrorCode.INVALID_STOCK_QUANTITY);
        }
    }

    private static void validateOrderQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BaseException(ProductErrorCode.INVALID_STOCK_QUANTITY);
        }
    }

    private static void validateCompany(Company company) {
        if (company == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }
}
