package com.boxoffice.ainotificationservice.notification.entity.message;

import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;

// Slack 수신자 식별 VO. id는 Slack User ID 또는 Channel ID.
public record Recipient(String id, RecipientType type) {

    public Recipient {
        if (id == null || id.isBlank() || type == null) {
            throw new BaseException(NotificationErrorCode.INVALID_RECIPIENT);
        }
    }

    public static Recipient user(String id) {
        return new Recipient(id, RecipientType.USER);
    }

    public static Recipient channel(String id) {
        return new Recipient(id, RecipientType.CHANNEL);
    }
}
