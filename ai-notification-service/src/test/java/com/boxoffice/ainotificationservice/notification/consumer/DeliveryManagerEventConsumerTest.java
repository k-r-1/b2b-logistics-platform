package com.boxoffice.ainotificationservice.notification.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineContext;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import com.boxoffice.ainotificationservice.ai.deadline.OrderLine;
import com.boxoffice.ainotificationservice.ai.deadline.WorkingHours;
import com.boxoffice.ainotificationservice.ai.service.DispatchDeadlinePredictor;
import com.boxoffice.ainotificationservice.notification.consumer.event.DeliveryAssignedEvent;
import com.boxoffice.ainotificationservice.notification.entity.message.EventCause;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.repository.ProcessedEventRepository;
import com.boxoffice.ainotificationservice.notification.service.NotificationService;
import com.boxoffice.ainotificationservice.notification.template.DispatchDeadlineNotificationContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("DeliveryManagerEventConsumer")
@ExtendWith(MockitoExtension.class)
class DeliveryManagerEventConsumerTest {

    private static final String CHANNEL = "C-TEST";

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DispatchDeadlinePredictor predictor;

    private DeliveryManagerEventConsumer consumer;

    @BeforeEach
    void setUp() {
        IdempotentEventProcessor idempotentProcessor = new IdempotentEventProcessor(processedEventRepository);
        consumer = new DeliveryManagerEventConsumer(
                idempotentProcessor, notificationService, predictor, CHANNEL);
    }

    @Test
    @DisplayName("DeliveryAssigned - 이벤트를 예측 입력으로 매핑하고, 예측 결과를 알림으로 발송")
    void delivery_assigned_predicts_and_sends() {
        // given
        given(processedEventRepository.existsByEventIdAndConsumerGroup(any(), any())).willReturn(false);
        LocalDateTime deadline = LocalDateTime.of(2026, 6, 1, 14, 30);
        given(predictor.predict(any()))
                .willReturn(DispatchDeadlinePrediction.llm(deadline, "납기 역산 결과", 0.9));
        DeliveryAssignedEvent event = new DeliveryAssignedEvent(
                "evt-da-1",
                new DeliveryAssignedEvent.Order(
                        "ORD-1234",
                        List.of(new DeliveryAssignedEvent.Product("고등어", 10)),
                        "냉장 보관 필수",
                        "2026-06-01T18:00:00+09:00"),
                new DeliveryAssignedEvent.Route("서울 중부센터", List.of("대전허브"), "부산 해운대구 센텀로 99"),
                10800L,
                new DeliveryAssignedEvent.Agent("김배송", new DeliveryAssignedEvent.WorkingHours("09:00", "18:00")));

        // when
        consumer.consume(event);

        // then - 예측 입력 매핑 검증
        ArgumentCaptor<DispatchDeadlineContext> captor = ArgumentCaptor.forClass(DispatchDeadlineContext.class);
        then(predictor).should().predict(captor.capture());
        DispatchDeadlineContext input = captor.getValue();
        assertThat(input.requestedDeadline()).isEqualTo(LocalDateTime.of(2026, 6, 1, 18, 0));
        assertThat(input.products()).containsExactly(new OrderLine("고등어", 10));
        assertThat(input.route().destination()).isEqualTo("부산 해운대구 센텀로 99");
        assertThat(input.totalEstimatedDuration()).isEqualTo(Duration.ofSeconds(10800));
        assertThat(input.agentWorkingHours()).isEqualTo(new WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0)));

        // then - 예측 결과 발송 검증
        then(notificationService).should().sendFromEvent(
                eq("evt-da-1"),
                eq(Recipient.channel(CHANNEL)),
                eq(new DispatchDeadlineNotificationContext("김배송", "ORD-1234", deadline, "납기 역산 결과")),
                eq(new EventCause("evt-da-1", "delivery-manager-service")));
    }

    @Test
    @DisplayName("DeliveryAssigned - 상세 정보가 비어있으면 더미로 채워 발송한다 (계약 합의 전 임시)")
    void delivery_assigned_without_detail_uses_dummy() {
        // given - order/route/agent 누락(thin) 이벤트
        given(processedEventRepository.existsByEventIdAndConsumerGroup(any(), any())).willReturn(false);
        LocalDateTime deadline = LocalDateTime.of(2026, 6, 1, 14, 30);
        given(predictor.predict(any()))
                .willReturn(DispatchDeadlinePrediction.llm(deadline, "더미 기반 예측", 0.5));
        DeliveryAssignedEvent thin = new DeliveryAssignedEvent("evt-thin", null, null, 0L, null);

        // when
        consumer.consume(thin);

        // then - 더미 입력으로 예측이 호출됨
        ArgumentCaptor<DispatchDeadlineContext> captor = ArgumentCaptor.forClass(DispatchDeadlineContext.class);
        then(predictor).should().predict(captor.capture());
        DispatchDeadlineContext input = captor.getValue();
        assertThat(input.products()).isNotEmpty();
        assertThat(input.route().destination()).isNotBlank();
        assertThat(input.agentWorkingHours()).isNotNull();

        // then - 더미 이름/주문번호로 발송됨
        then(notificationService).should().sendFromEvent(
                eq("evt-thin"),
                eq(Recipient.channel(CHANNEL)),
                eq(new DispatchDeadlineNotificationContext("데모담당자", "DEMO-ORDER", deadline, "더미 기반 예측")),
                eq(new EventCause("evt-thin", "delivery-manager-service")));
    }
}
