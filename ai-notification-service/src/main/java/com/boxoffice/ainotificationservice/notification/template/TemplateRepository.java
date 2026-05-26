package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;

import java.util.Optional;

public interface TemplateRepository {

    Optional<String> findBody(TemplateType type);
}
