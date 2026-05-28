package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;

public record MasterSignupRequestContext(String name, String email, String role) implements TemplateContext {

    public MasterSignupRequestContext {
        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || role == null || role.isBlank()) {
            throw new BaseException(NotificationErrorCode.TEMPLATE_CONTEXT_MISSING);
        }
    }

    @Override
    public TemplateType type() {
        return TemplateType.MASTER_SIGNUP_REQUEST;
    }

    @Override
    public String render(String bodyTemplate) {
        return bodyTemplate
                .replace("{name}", name)
                .replace("{email}", email)
                .replace("{role}", role);
    }
}
