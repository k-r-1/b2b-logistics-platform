package com.boxoffice.ainotificationservice.notification.consumer;

import com.boxoffice.ainotificationservice.notification.consumer.event.DeliveryEvent;
import com.boxoffice.ainotificationservice.notification.consumer.event.DeliveryStatusChangedEvent;
import com.boxoffice.ainotificationservice.notification.entity.message.EventCause;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.service.NotificationService;
import com.boxoffice.ainotificationservice.notification.template.DeliveryStatusContext;
import com.boxoffice.ainotificationservice.notification.template.TemplateContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// delivery.events 토픽 컨슈머. eventType 다형성으로 구체 이벤트를 수신 → 멱등 처리 → 단일 채널 발송.
@Component
public class DeliveryEventConsumer {

    private static final String GROUP = "ai-notification-service";
    private static final String SOURCE = "delivery-service";

    private final IdempotentEventProcessor idempotentProcessor;
    private final NotificationService notificationService;
    private final String channelId;

    public DeliveryEventConsumer(
            IdempotentEventProcessor idempotentProcessor,
            NotificationService notificationService,
            @Value("${notification.slack.channel-id}") String channelId) {
        this.idempotentProcessor = idempotentProcessor;
        this.notificationService = notificationService;
        this.channelId = channelId;
    }

    @KafkaListener(topics = "delivery.events", groupId = GROUP)
    public void consume(DeliveryEvent event) {
        TemplateContext context = switch (event) {
            case DeliveryStatusChangedEvent e -> new DeliveryStatusContext(
                    e.deliveryId(), e.orderId(), e.status(), e.recipientName(), e.failureReason());
        };
        dispatch(event.eventId(), context);
    }

    private void dispatch(String eventId, TemplateContext context) {
        idempotentProcessor.processOnce(eventId, GROUP, () ->
                notificationService.sendFromEvent(
                        eventId, Recipient.channel(channelId), context, new EventCause(eventId, SOURCE)));
    }
}
