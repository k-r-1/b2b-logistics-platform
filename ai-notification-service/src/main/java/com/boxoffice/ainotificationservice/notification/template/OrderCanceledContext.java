package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;

// ordererName·hubManagerName은 계약상 선택 필드(본문 표시용). 미제공 시 "-"로 표기.
public record OrderCanceledContext(String orderId, String reason, String ordererName, String hubManagerName)
        implements TemplateContext {

    public OrderCanceledContext {
        if (orderId == null || orderId.isBlank() || reason == null || reason.isBlank()) {
            throw new BaseException(NotificationErrorCode.TEMPLATE_CONTEXT_MISSING);
        }
    }

    public OrderCanceledContext(String orderId, String reason) {
        this(orderId, reason, null, null);
    }

    @Override
    public TemplateType type() {
        return TemplateType.ORDER_CANCELED;
    }

    @Override
    public String render(String bodyTemplate) {
        return bodyTemplate
                .replace("{orderId}", orderId)
                .replace("{reason}", reason)
                .replace("{ordererName}", ordererName != null ? ordererName : "-")
                .replace("{hubManagerName}", hubManagerName != null ? hubManagerName : "-");
    }
}
