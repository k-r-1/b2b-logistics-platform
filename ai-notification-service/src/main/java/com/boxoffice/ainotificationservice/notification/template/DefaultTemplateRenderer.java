package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;
import org.springframework.stereotype.Component;

@Component
public class DefaultTemplateRenderer implements TemplateRenderer {

    private final TemplateRepository templateRepository;

    public DefaultTemplateRenderer(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public String render(TemplateContext context) {
        String body = templateRepository.findBody(context.type())
                .orElseThrow(() -> new BaseException(NotificationErrorCode.TEMPLATE_NOT_FOUND));
        return context.render(body);
    }
}
