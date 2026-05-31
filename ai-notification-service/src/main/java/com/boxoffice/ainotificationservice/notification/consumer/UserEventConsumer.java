package com.boxoffice.ainotificationservice.notification.consumer;

import com.boxoffice.ainotificationservice.notification.consumer.event.UserApprovedEvent;
import com.boxoffice.ainotificationservice.notification.consumer.event.UserEvent;
import com.boxoffice.ainotificationservice.notification.consumer.event.UserRejectedEvent;
import com.boxoffice.ainotificationservice.notification.consumer.event.UserSignupRequestedEvent;
import com.boxoffice.ainotificationservice.notification.entity.message.EventCause;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.service.NotificationService;
import com.boxoffice.ainotificationservice.notification.template.MasterSignupRequestContext;
import com.boxoffice.ainotificationservice.notification.template.TemplateContext;
import com.boxoffice.ainotificationservice.notification.template.UserApprovedContext;
import com.boxoffice.ainotificationservice.notification.template.UserRejectedContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// user.events 토픽 컨슈머. eventType 다형성으로 구체 이벤트를 수신 → 멱등 처리 → 단일 채널 발송.
@Component
public class UserEventConsumer {

    private static final String GROUP = "ai-notification-service";
    private static final String SOURCE = "user-service";

    private final IdempotentEventProcessor idempotentProcessor;
    private final NotificationService notificationService;
    private final String channelId;

    public UserEventConsumer(
            IdempotentEventProcessor idempotentProcessor,
            NotificationService notificationService,
            @Value("${notification.slack.channel-id}") String channelId) {
        this.idempotentProcessor = idempotentProcessor;
        this.notificationService = notificationService;
        this.channelId = channelId;
    }

    @KafkaListener(topics = "user.events", groupId = GROUP)
    public void consume(UserEvent event) {
        TemplateContext context = switch (event) {
            case UserSignupRequestedEvent e ->
                    new MasterSignupRequestContext(e.applicantName(), e.email(), e.requestedRole());
            case UserApprovedEvent e -> new UserApprovedContext(e.userName());
            case UserRejectedEvent e -> new UserRejectedContext(e.userName(), e.reason());
        };
        dispatch(event.eventId(), context);
    }

    private void dispatch(String eventId, TemplateContext context) {
        idempotentProcessor.processOnce(eventId, GROUP, () ->
                notificationService.sendFromEvent(
                        eventId, Recipient.channel(channelId), context, new EventCause(eventId, SOURCE)));
    }
}
