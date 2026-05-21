package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;

public record UserApprovedContext(String name) implements TemplateContext {

    public UserApprovedContext {
        if (name == null || name.isBlank()) {
            throw new BaseException(NotificationErrorCode.TEMPLATE_CONTEXT_MISSING);
        }
    }

    @Override
    public TemplateType type() {
        return TemplateType.USER_APPROVED;
    }

    @Override
    public String render(String bodyTemplate) {
        return bodyTemplate.replace("{name}", name);
    }
}
