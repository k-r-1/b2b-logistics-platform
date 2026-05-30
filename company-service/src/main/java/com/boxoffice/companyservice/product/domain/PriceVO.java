package com.boxoffice.companyservice.product.domain;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.companyservice.product.exception.ProductErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceVO {

    @Column(name = "price", nullable = false)
    private Integer value;

    private PriceVO(Integer value) {
        this.value = value;
    }

    public static PriceVO create(Integer value) {
        validate(value);
        return new PriceVO(value);
    }

    private static void validate(Integer value) {
        // 공통 핸들러가 IllegalArgumentException을 400으로 변환하지 않으므로 최소 방어선도 BaseException으로 맞춘다.
        if (value == null) {
            throw new BaseException(ProductErrorCode.INVALID_PRODUCT_PRICE);
        }

        if (value < 0) {
            throw new BaseException(ProductErrorCode.INVALID_PRODUCT_PRICE);
        }
    }
}
