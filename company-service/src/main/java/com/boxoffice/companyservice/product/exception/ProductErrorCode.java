package com.boxoffice.companyservice.product.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    INVALID_PRODUCT_NAME(HttpStatus.BAD_REQUEST, "PRODUCT-001", "상품명이 올바르지 않습니다."),
    INVALID_PRODUCT_PRICE(HttpStatus.BAD_REQUEST, "PRODUCT-002", "상품 가격이 올바르지 않습니다."),
    INVALID_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "PRODUCT-003", "상품 재고 수량이 올바르지 않습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-004", "존재하지 않는 상품입니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT-005", "상품 재고가 부족합니다."),
    STOCK_OPERATION_IN_PROGRESS(HttpStatus.CONFLICT, "PRODUCT-006", "상품 재고 작업이 이미 처리 중입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
