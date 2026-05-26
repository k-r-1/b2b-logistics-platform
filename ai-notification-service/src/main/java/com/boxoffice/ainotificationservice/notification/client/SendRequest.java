package com.boxoffice.ainotificationservice.notification.client;

import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;

public record SendRequest(Recipient recipient, String body) {

    public SendRequest {
        if (recipient == null) {
            throw new BaseException(NotificationErrorCode.INVALID_RECIPIENT);
        }
        if (body == null || body.isBlank()) {
            throw new BaseException(NotificationErrorCode.INVALID_MESSAGE_BODY);
        }
    }
}
