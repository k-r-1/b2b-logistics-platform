package com.boxoffice.ainotificationservice.notification.consumer;

import com.boxoffice.ainotificationservice.ai.deadline.DeliveryRoute;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineContext;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import com.boxoffice.ainotificationservice.ai.deadline.OrderLine;
import com.boxoffice.ainotificationservice.ai.deadline.WorkingHours;
import com.boxoffice.ainotificationservice.ai.service.DispatchDeadlinePredictor;
import com.boxoffice.ainotificationservice.notification.consumer.event.DeliveryAssignedEvent;
import com.boxoffice.ainotificationservice.notification.consumer.event.DeliveryManagerEvent;
import com.boxoffice.ainotificationservice.notification.entity.message.EventCause;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.service.NotificationService;
import com.boxoffice.ainotificationservice.notification.template.DispatchDeadlineNotificationContext;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// delivery-manager.events 토픽 컨슈머. DeliveryAssigned 수신 → AI 발송 시한 예측 → 단일 채널 발송.
@Component
public class DeliveryManagerEventConsumer {

    private static final String GROUP = "ai-notification-service";
    private static final String SOURCE = "delivery-manager-service";

    // delivery-manager 이벤트가 주문·경로 상세를 싣기 전까지의 임시 더미.
    //  계약 합의 후 이 상수들과 withDummyDefaults()를 통째로 제거.
    private static final DeliveryAssignedEvent.Order DUMMY_ORDER = new DeliveryAssignedEvent.Order(
            "DEMO-ORDER",
            List.of(new DeliveryAssignedEvent.Product("샘플상품", 1)),
            null,
            "2026-06-01T18:00:00+09:00");
    private static final DeliveryAssignedEvent.Route DUMMY_ROUTE = new DeliveryAssignedEvent.Route(
            "출발허브", List.of(), "도착허브");
    private static final DeliveryAssignedEvent.Agent DUMMY_AGENT = new DeliveryAssignedEvent.Agent(
            "데모담당자", new DeliveryAssignedEvent.WorkingHours("09:00", "18:00"));
    private static final long DUMMY_DURATION_SECONDS = 10800L;

    private final IdempotentEventProcessor idempotentProcessor;
    private final NotificationService notificationService;
    private final DispatchDeadlinePredictor predictor;
    private final String channelId;

    public DeliveryManagerEventConsumer(
            IdempotentEventProcessor idempotentProcessor,
            NotificationService notificationService,
            DispatchDeadlinePredictor predictor,
            @Value("${notification.slack.channel-id}") String channelId) {
        this.idempotentProcessor = idempotentProcessor;
        this.notificationService = notificationService;
        this.predictor = predictor;
        this.channelId = channelId;
    }

    @KafkaListener(topics = "delivery-manager.events", groupId = GROUP)
    public void consume(DeliveryManagerEvent event) {
        switch (event) {
            case DeliveryAssignedEvent e -> handleDeliveryAssigned(e);
        }
    }

    private void handleDeliveryAssigned(DeliveryAssignedEvent event) {
        DeliveryAssignedEvent enriched = withDummyDefaults(event);

        DispatchDeadlinePrediction prediction = predictor.predict(toPredictionContext(enriched));
        DispatchDeadlineNotificationContext context = new DispatchDeadlineNotificationContext(
                enriched.agent().name(),
                enriched.order().orderId(),
                prediction.dispatchDeadline(),
                prediction.reasoning());
        idempotentProcessor.processOnce(event.eventId(), GROUP, () ->
                notificationService.sendFromEvent(
                        event.eventId(), Recipient.channel(channelId), context,
                        new EventCause(event.eventId(), SOURCE)));
    }

    // (임시) order/route/agent가 비어 오면 더미로 채워 예측·발송을 동작.
    private DeliveryAssignedEvent withDummyDefaults(DeliveryAssignedEvent event) {
        return new DeliveryAssignedEvent(
                event.eventId(),
                event.order() != null ? event.order() : DUMMY_ORDER,
                event.route() != null ? event.route() : DUMMY_ROUTE,
                event.totalEstimatedDurationSeconds() > 0 ? event.totalEstimatedDurationSeconds() : DUMMY_DURATION_SECONDS,
                event.agent() != null ? event.agent() : DUMMY_AGENT);
    }

    private DispatchDeadlineContext toPredictionContext(DeliveryAssignedEvent event) {
        DeliveryAssignedEvent.Order order = event.order();
        List<OrderLine> products = order.products().stream()
                .map(p -> new OrderLine(p.name(), p.quantity()))
                .toList();
        DeliveryRoute route = new DeliveryRoute(
                event.route().origin(), event.route().waypoints(), event.route().destination());
        WorkingHours workingHours = new WorkingHours(
                LocalTime.parse(event.agent().workingHours().start()),
                LocalTime.parse(event.agent().workingHours().end()));
        return new DispatchDeadlineContext(
                OffsetDateTime.parse(order.requestedDeadline()).toLocalDateTime(),
                order.requesterNote(),
                products,
                route,
                Duration.ofSeconds(event.totalEstimatedDurationSeconds()),
                workingHours);
    }
}
