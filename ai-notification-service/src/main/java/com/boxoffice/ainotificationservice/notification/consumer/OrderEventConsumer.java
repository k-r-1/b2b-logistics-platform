package com.boxoffice.ainotificationservice.notification.consumer;

import com.boxoffice.ainotificationservice.notification.consumer.event.OrderCanceledEvent;
import com.boxoffice.ainotificationservice.notification.consumer.event.OrderEvent;
import com.boxoffice.ainotificationservice.notification.entity.message.EventCause;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.service.NotificationService;
import com.boxoffice.ainotificationservice.notification.template.OrderCanceledContext;
import com.boxoffice.ainotificationservice.notification.template.TemplateContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// order.events 토픽 컨슈머. eventType 다형성으로 구체 이벤트를 수신 → 멱등 처리 → 단일 채널 발송.
@Component
public class OrderEventConsumer {

    private static final String GROUP = "ai-notification-service";
    private static final String SOURCE = "order-service";

    private final IdempotentEventProcessor idempotentProcessor;
    private final NotificationService notificationService;
    private final String channelId;

    public OrderEventConsumer(
            IdempotentEventProcessor idempotentProcessor,
            NotificationService notificationService,
            @Value("${notification.slack.channel-id}") String channelId) {
        this.idempotentProcessor = idempotentProcessor;
        this.notificationService = notificationService;
        this.channelId = channelId;
    }

    @KafkaListener(topics = "order.events", groupId = GROUP)
    public void consume(OrderEvent event) {
        TemplateContext context = switch (event) {
            case OrderCanceledEvent e -> new OrderCanceledContext(
                    e.orderId(), e.reason(), e.ordererName(), e.hubManagerName());
        };
        dispatch(event.eventId(), context);
    }

    private void dispatch(String eventId, TemplateContext context) {
        idempotentProcessor.processOnce(eventId, GROUP, () ->
                notificationService.sendFromEvent(
                        eventId, Recipient.channel(channelId), context, new EventCause(eventId, SOURCE)));
    }
}
