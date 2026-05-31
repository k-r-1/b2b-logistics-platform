package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// AI 발송 시한 예측 결과 알림. 담당자에게 "언제까지 발송해야 하는지"를 전달한다.
public record DispatchDeadlineNotificationContext(
        String agentName,
        String orderId,
        LocalDateTime dispatchDeadline,
        String reasoning
) implements TemplateContext {

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public DispatchDeadlineNotificationContext {
        if (agentName == null || agentName.isBlank()
                || orderId == null || orderId.isBlank()
                || dispatchDeadline == null) {
            throw new BaseException(NotificationErrorCode.TEMPLATE_CONTEXT_MISSING);
        }
    }

    @Override
    public TemplateType type() {
        return TemplateType.DISPATCH_DEADLINE;
    }

    @Override
    public String render(String bodyTemplate) {
        return bodyTemplate
                .replace("{agentName}", agentName)
                .replace("{orderId}", orderId)
                .replace("{deadline}", dispatchDeadline.format(DEADLINE_FORMAT))
                .replace("{reasoning}", reasoning != null ? reasoning : "-");
    }
}
