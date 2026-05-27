package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;

public sealed interface TemplateContext permits
        MasterSignupRequestContext,
        UserApprovedContext,
        UserRejectedContext,
        OrderCanceledContext {

    TemplateType type();

    String render(String bodyTemplate);
}
