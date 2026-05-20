package com.boxoffice.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 모든 서비스에서 공통으로 사용하는 에러 코드.
 * ErrorCode 인터페이스를 구현한다.
 *
 * 각 서비스의 도메인 에러 코드는 해당 서비스에서 별도로 관리한다.
 *
 * 도메인별 에러 코드 예시:
 *   hub-service 내부에 HubErrorCode enum 별도 정의
 *   public enum HubErrorCode implements ErrorCode { ... }
 */
@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // 공통
    /** 요청 파라미터 형식 오류. @Valid 검증 실패 시 사용. */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON-001", "요청 파라미터 형식 오류"),

    /** 서버 내부 오류. 예상치 못한 예외 발생 시 사용. */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-002", "서버 내부 오류"),

    // 인증/인가
    /** JWT 토큰 없음 또는 유효하지 않음. */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON-003", "인증 실패"),

    /** 접근 권한 없음. */
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON-004", "권한 없음"),

    // 서비스 간 통신
    /** FeignClient 호출 실패. */
    FEIGN_CLIENT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-005", "서비스 간 호출 실패");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}