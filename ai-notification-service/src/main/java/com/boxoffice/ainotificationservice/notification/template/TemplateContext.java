package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;

public sealed interface TemplateContext permits
        MasterSignupRequestContext,
        UserApprovedContext,
        UserRejectedContext,
        OrderCanceledContext,
        DeliveryStatusContext,
        DispatchDeadlineNotificationContext {

    TemplateType type();

    String render(String bodyTemplate);
}
