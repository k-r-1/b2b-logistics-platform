package com.boxoffice.userservice.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // 400 BAD_REQUEST 에러
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "USER_001", "이미 가입된 이메일 주소입니다."),



    // 500 INTERNAL_SERVER_ERROR 에러
    KEYCLOAK_ADMIN_AUTH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_002", "인증 서버 관리자 인증에 실패했습니다."),
    KEYCLOAK_USER_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_003", "인증 서버에 사용자 등록을 실패했습니다."),
    KEYCLOAK_LOCATION_HEADER_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "USER_004", "인증 서버 응답 규격이 올바르지 않습니다."),

    // 401 UNAUTHORIZED 에러
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USER_005", "아이디 또는 비밀번호가 일치하지 않습니다."),

    // 404 NOT_FOUND 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_006", "존재하지 않는 사용자입니다."),
    FORBIDDEN_ACCESS(HttpStatus.NOT_FOUND, "USER_007", "마스터 권한으로 다시 시도해주세요.");
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}