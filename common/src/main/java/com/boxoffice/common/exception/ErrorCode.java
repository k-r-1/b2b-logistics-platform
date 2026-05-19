package com.boxoffice.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 모든 도메인 에러 코드의 공통 인터페이스.
 *
 * 각 서비스는 이 인터페이스를 구현한 Enum을 정의한다.
 * GlobalExceptionHandler가 이 인터페이스 기반으로
 * 모든 서비스의 에러를 통일하여 처리한다.
 *
 * 사용 예시:
 *   @Getter
 *   @AllArgsConstructor
 *   public enum HubErrorCode implements ErrorCode {
 *       HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB-001", "허브를 찾을 수 없습니다.");
 *
 *       private final HttpStatus httpStatus;
 *       private final String code;
 *       private final String message;
 *   }
 *
 *   throw new BaseException(HubErrorCode.HUB_NOT_FOUND);
 */
public interface ErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}