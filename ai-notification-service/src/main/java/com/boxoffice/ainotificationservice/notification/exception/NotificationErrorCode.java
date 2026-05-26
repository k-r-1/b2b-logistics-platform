package com.boxoffice.ainotificationservice.notification.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "NOTI-001", "유효하지 않은 알림 상태 전이입니다."),
    DUPLICATE_IDEMPOTENCY_KEY(HttpStatus.CONFLICT, "NOTI-002", "이미 처리된 멱등성 키입니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI-003", "알림을 찾을 수 없습니다."),
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI-004", "등록되지 않은 템플릿 종류입니다."),
    TEMPLATE_CONTEXT_MISSING(HttpStatus.BAD_REQUEST, "NOTI-005", "템플릿 컨텍스트에 필수 키가 누락되었습니다."),
    INVALID_RECIPIENT(HttpStatus.BAD_REQUEST, "NOTI-006", "수신자 정보가 유효하지 않습니다."),
    INVALID_EVENT_CAUSE(HttpStatus.BAD_REQUEST, "NOTI-007", "원인 이벤트 정보가 유효하지 않습니다."),
    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "NOTI-008", "멱등성 키가 유효하지 않습니다."),
    INVALID_MESSAGE_BODY(HttpStatus.BAD_REQUEST, "NOTI-009", "메시지 본문이 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
