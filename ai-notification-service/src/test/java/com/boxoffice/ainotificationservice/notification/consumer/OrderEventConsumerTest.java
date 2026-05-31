package com.boxoffice.ainotificationservice.notification.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.boxoffice.ainotificationservice.notification.consumer.event.OrderCanceledEvent;
import com.boxoffice.ainotificationservice.notification.entity.message.EventCause;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.repository.ProcessedEventRepository;
import com.boxoffice.ainotificationservice.notification.service.NotificationService;
import com.boxoffice.ainotificationservice.notification.template.OrderCanceledContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("OrderEventConsumer")
@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    private static final String CHANNEL = "C-TEST";

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationService notificationService;

    private OrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        given(processedEventRepository.existsByEventIdAndConsumerGroup(any(), any())).willReturn(false);
        IdempotentEventProcessor idempotentProcessor = new IdempotentEventProcessor(processedEventRepository);
        consumer = new OrderEventConsumer(idempotentProcessor, notificationService, CHANNEL);
    }

    @Test
    @DisplayName("OrderCanceled - 단일 채널로 OrderCanceledContext 발송")
    void order_canceled() {
        consumer.consume(new OrderCanceledEvent("evt-1", "ORD-100", "재고 부족", "홍길동", "김허브"));

        then(notificationService).should().sendFromEvent(
                eq("evt-1"),
                eq(Recipient.channel(CHANNEL)),
                eq(new OrderCanceledContext("ORD-100", "재고 부족", "홍길동", "김허브")),
                eq(new EventCause("evt-1", "order-service")));
    }
}
