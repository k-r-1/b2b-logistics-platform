package com.boxoffice.ainotificationservice.notification.service;

import com.boxoffice.ainotificationservice.notification.entity.message.EventCause;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.entity.message.SlackMessage;
import com.boxoffice.ainotificationservice.notification.repository.SlackMessageRepository;
import com.boxoffice.ainotificationservice.notification.template.TemplateContext;
import com.boxoffice.ainotificationservice.notification.template.TemplateRenderer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SlackMessageRepository repository;
    private final TemplateRenderer renderer;
    private final ApplicationEventPublisher publisher;

    // 직접 발송
    @Transactional
    public SlackMessage sendDirect(String idempotencyKey, Recipient recipient, TemplateContext context) {
        return idempotentOrEnqueue(idempotencyKey, () -> {
            String body = renderer.render(context);
            SlackMessage message = SlackMessage.direct(idempotencyKey, recipient, context.type(), body);
            return saveAndPublish(message, body, recipient);
        });
    }

    // 이벤트 트리거 발송. cause 필수 (SlackMessage.fromEvent에서 검증).
    @Transactional
    public SlackMessage sendFromEvent(
            String idempotencyKey,
            Recipient recipient,
            TemplateContext context,
            EventCause cause) {
        return idempotentOrEnqueue(idempotencyKey, () -> {
            String body = renderer.render(context);
            SlackMessage message = SlackMessage.fromEvent(idempotencyKey, recipient, context.type(), body, cause);
            return saveAndPublish(message, body, recipient);
        });
    }

    private SlackMessage idempotentOrEnqueue(String idempotencyKey, Supplier<SlackMessage> enqueue) {
        return repository.findByIdempotencyKey(idempotencyKey).orElseGet(enqueue);
    }

    // PENDING 저장 + 이벤트 발행. 발송은 SlackMessageDispatcher가 AFTER_COMMIT에 처리하여 트랜잭션 외부에서 실행
    private SlackMessage saveAndPublish(SlackMessage message, String body, Recipient recipient) {
        repository.save(message);
        publisher.publishEvent(new SlackMessageQueuedEvent(message.getId(), body, recipient));
        return message;
    }
}
