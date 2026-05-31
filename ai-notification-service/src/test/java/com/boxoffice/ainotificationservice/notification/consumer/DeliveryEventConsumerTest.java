package com.boxoffice.ainotificationservice.notification.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.boxoffice.ainotificationservice.notification.consumer.event.DeliveryStatusChangedEvent;
import com.boxoffice.ainotificationservice.notification.entity.message.EventCause;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.repository.ProcessedEventRepository;
import com.boxoffice.ainotificationservice.notification.service.NotificationService;
import com.boxoffice.ainotificationservice.notification.template.DeliveryStatus;
import com.boxoffice.ainotificationservice.notification.template.DeliveryStatusContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("DeliveryEventConsumer")
@ExtendWith(MockitoExtension.class)
class DeliveryEventConsumerTest {

    private static final String CHANNEL = "C-TEST";

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationService notificationService;

    private DeliveryEventConsumer consumer;

    @BeforeEach
    void setUp() {
        given(processedEventRepository.existsByEventIdAndConsumerGroup(any(), any())).willReturn(false);
        IdempotentEventProcessor idempotentProcessor = new IdempotentEventProcessor(processedEventRepository);
        consumer = new DeliveryEventConsumer(idempotentProcessor, notificationService, CHANNEL);
    }

    @Test
    @DisplayName("DeliveryStatusChanged(FAILED) - DeliveryStatusContext 발송")
    void delivery_status_failed() {
        consumer.consume(new DeliveryStatusChangedEvent(
                "evt-1", "DLV-1", "ORD-1", DeliveryStatus.FAILED, null, "주소 불명"));

        then(notificationService).should().sendFromEvent(
                eq("evt-1"),
                eq(Recipient.channel(CHANNEL)),
                eq(new DeliveryStatusContext("DLV-1", "ORD-1", DeliveryStatus.FAILED, null, "주소 불명")),
                eq(new EventCause("evt-1", "delivery-service")));
    }
}
