package com.boxoffice.ainotificationservice.notification.entity.message;

import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;

import java.util.Optional;

// 알림을 유발한 원인 이벤트 역추적 정보 (Kafka 이벤트 등). 직접 발송 API에서는 비어 있음.
public record EventCause(String eventId, String source) {

    public EventCause {
        if (eventId == null || eventId.isBlank() || source == null || source.isBlank()) {
            throw new BaseException(NotificationErrorCode.INVALID_EVENT_CAUSE);
        }
    }

    // 둘 다 null이면 empty (직접 발송 API), 부분 null은 검증 실패.
    public static Optional<EventCause> optional(String eventId, String source) {
        if (eventId == null && source == null) {
            return Optional.empty();
        }
        return Optional.of(new EventCause(eventId, source));
    }
}
