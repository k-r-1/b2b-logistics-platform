package com.boxoffice.user_service.exception;

import com.boxoffice.common.exception.ErrorCode; // 팀원의 공통 인터페이스 임포트
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // 400 BAD_REQUEST 에러 모음
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "USER_001", "이미 가입된 이메일 주소입니다."),



    // 500 INTERNAL_SERVER_ERROR 에러 모음
    KEYCLOAK_ADMIN_AUTH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_002", "인증 서버 관리자 인증에 실패했습니다."),
    KEYCLOAK_USER_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_003", "인증 서버에 사용자 등록을 실패했습니다."),
    KEYCLOAK_LOCATION_HEADER_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "USER_004", "인증 서버 응답 규격이 올바르지 않습니다."),

    // 401 UNAUTHORIZED 에러 추가
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USER_005", "아이디 또는 비밀번호가 일치하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}