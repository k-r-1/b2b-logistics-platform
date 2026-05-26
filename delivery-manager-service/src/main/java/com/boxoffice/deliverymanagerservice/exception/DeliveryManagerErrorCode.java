package com.boxoffice.deliverymanagerservice.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DeliveryManagerErrorCode implements ErrorCode {

    // 404 NOT_FOUND
    DELIVERY_MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_MANAGER_001", "해당 배송 담당자를 찾을 수 없습니다."),

    // 400 BAD_REQUEST
    ALREADY_REGISTERED_MANAGER(HttpStatus.BAD_REQUEST, "DELIVERY_MANAGER_002", "이미 배송 담당자로 등록된 유저입니다."),
    INVALID_HUB_OR_TYPE(HttpStatus.BAD_REQUEST, "DELIVERY_MANAGER_003", "허브 정보 또는 배송 타입이 올바르지 않습니다."),

    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "DELIVERY_MANAGER_004", "해당 요청에 대한 접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}