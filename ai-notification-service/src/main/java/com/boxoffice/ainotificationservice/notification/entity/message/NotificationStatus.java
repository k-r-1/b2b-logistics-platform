package com.boxoffice.ainotificationservice.notification.entity.message;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

// Slack 알림 처리 상태. 허용된 전이 표는 ALLOWED_NEXT에 정의.
public enum NotificationStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
    DLQ;

    private static final Map<NotificationStatus, Set<NotificationStatus>> ALLOWED_NEXT =
            new EnumMap<>(NotificationStatus.class);

    static {
        ALLOWED_NEXT.put(PENDING, EnumSet.of(SENDING, FAILED));
        ALLOWED_NEXT.put(SENDING, EnumSet.of(SENT, FAILED, PENDING));
        ALLOWED_NEXT.put(SENT, EnumSet.noneOf(NotificationStatus.class));
        ALLOWED_NEXT.put(FAILED, EnumSet.of(DLQ));
        ALLOWED_NEXT.put(DLQ, EnumSet.noneOf(NotificationStatus.class));
    }

    public boolean canTransitionTo(NotificationStatus next) {
        return ALLOWED_NEXT.get(this).contains(next);
    }

    // SENT 또는 DLQ. 재시도/복구 대상 아님.
    public boolean isTerminal() {
        return this == SENT || this == DLQ;
    }
}
