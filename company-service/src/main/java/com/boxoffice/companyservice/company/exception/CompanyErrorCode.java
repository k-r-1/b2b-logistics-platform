package com.boxoffice.companyservice.company.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CompanyErrorCode implements ErrorCode {

    HUB_NOT_FOUND(HttpStatus.BAD_REQUEST, "COMPANY-001", "존재하지 않는 허브입니다."),
    HUB_INACTIVE(HttpStatus.BAD_REQUEST, "COMPANY-002", "비활성화된 허브입니다."),
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY-003", "존재하지 않는 업체입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
