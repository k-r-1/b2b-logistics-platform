package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;

public record OrderCanceledContext(String orderId, String reason) implements TemplateContext {

    public OrderCanceledContext {
        if (orderId == null || orderId.isBlank() || reason == null || reason.isBlank()) {
            throw new BaseException(NotificationErrorCode.TEMPLATE_CONTEXT_MISSING);
        }
    }

    @Override
    public TemplateType type() {
        return TemplateType.ORDER_CANCELED;
    }

    @Override
    public String render(String bodyTemplate) {
        return bodyTemplate
                .replace("{orderId}", orderId)
                .replace("{reason}", reason);
    }
}
