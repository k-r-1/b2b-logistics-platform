package com.boxoffice.common.exception;

import lombok.Getter;

/**
 * 모든 커스텀 예외의 부모 클래스.
 *
 * ErrorCode 인터페이스를 구현한 Enum을 받아서 처리한다.
 * GlobalExceptionHandler에서 ApiResponse 형식으로 변환된다.
 *
 * 사용 예시:
 *   throw new BaseException(HubErrorCode.HUB_NOT_FOUND);
 *   throw new BaseException(CommonErrorCode.UNAUTHORIZED);
 */
@Getter
public class BaseException extends RuntimeException {

    /** 에러 코드 */
    private final ErrorCode errorCode;

    /**
     * ErrorCode 인터페이스를 구현한 Enum으로 예외를 생성한다.
     *
     * @param errorCode ErrorCode 구현체 (CommonErrorCode, HubErrorCode 등)
     */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}