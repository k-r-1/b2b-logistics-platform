package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;

public record UserRejectedContext(String name, String reason) implements TemplateContext {

    public UserRejectedContext {
        if (name == null || name.isBlank() || reason == null || reason.isBlank()) {
            throw new BaseException(NotificationErrorCode.TEMPLATE_CONTEXT_MISSING);
        }
    }

    @Override
    public TemplateType type() {
        return TemplateType.USER_REJECTED;
    }

    @Override
    public String render(String bodyTemplate) {
        return bodyTemplate
                .replace("{name}", name)
                .replace("{reason}", reason);
    }
}
