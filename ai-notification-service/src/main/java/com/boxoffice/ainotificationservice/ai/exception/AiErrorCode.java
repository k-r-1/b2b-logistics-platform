package com.boxoffice.ainotificationservice.ai.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiErrorCode implements ErrorCode {

    INVALID_DISPATCH_INPUT(HttpStatus.BAD_REQUEST, "AI-001", "발송 시한 예측 입력이 유효하지 않습니다."),
    INVALID_WORKING_HOURS(HttpStatus.BAD_REQUEST, "AI-002", "근무 시간대가 유효하지 않습니다."),
    INVALID_PREDICTION(HttpStatus.BAD_REQUEST, "AI-003", "예측 결과가 유효하지 않습니다."),
    LLM_CALL_FAILED(HttpStatus.BAD_GATEWAY, "AI-004", "LLM 호출에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
