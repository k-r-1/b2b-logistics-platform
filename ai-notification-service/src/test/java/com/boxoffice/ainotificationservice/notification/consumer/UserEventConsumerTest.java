package com.boxoffice.ainotificationservice.notification.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.boxoffice.ainotificationservice.notification.consumer.event.UserApprovedEvent;
import com.boxoffice.ainotificationservice.notification.consumer.event.UserRejectedEvent;
import com.boxoffice.ainotificationservice.notification.consumer.event.UserSignupRequestedEvent;
import com.boxoffice.ainotificationservice.notification.entity.message.EventCause;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.repository.ProcessedEventRepository;
import com.boxoffice.ainotificationservice.notification.service.NotificationService;
import com.boxoffice.ainotificationservice.notification.template.MasterSignupRequestContext;
import com.boxoffice.ainotificationservice.notification.template.UserApprovedContext;
import com.boxoffice.ainotificationservice.notification.template.UserRejectedContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("UserEventConsumer")
@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    private static final String CHANNEL = "C-TEST";

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationService notificationService;

    private UserEventConsumer consumer;

    @BeforeEach
    void setUp() {
        given(processedEventRepository.existsByEventIdAndConsumerGroup(any(), any())).willReturn(false);
        IdempotentEventProcessor idempotentProcessor = new IdempotentEventProcessor(processedEventRepository);
        consumer = new UserEventConsumer(idempotentProcessor, notificationService, CHANNEL);
    }

    @Test
    @DisplayName("UserApproved - 단일 채널로 UserApprovedContext 발송")
    void user_approved() {
        consumer.consume(new UserApprovedEvent("evt-1", "홍길동"));

        then(notificationService).should().sendFromEvent(
                eq("evt-1"),
                eq(Recipient.channel(CHANNEL)),
                eq(new UserApprovedContext("홍길동")),
                eq(new EventCause("evt-1", "user-service")));
    }

    @Test
    @DisplayName("UserSignupRequested - MasterSignupRequestContext 발송")
    void user_signup_requested() {
        consumer.consume(new UserSignupRequestedEvent("evt-2", "김지원", "jiwon@example.com", "MASTER"));

        then(notificationService).should().sendFromEvent(
                eq("evt-2"),
                eq(Recipient.channel(CHANNEL)),
                eq(new MasterSignupRequestContext("김지원", "jiwon@example.com", "MASTER")),
                eq(new EventCause("evt-2", "user-service")));
    }

    @Test
    @DisplayName("UserRejected - UserRejectedContext 발송")
    void user_rejected() {
        consumer.consume(new UserRejectedEvent("evt-3", "이서준", "서류 미비"));

        then(notificationService).should().sendFromEvent(
                eq("evt-3"),
                eq(Recipient.channel(CHANNEL)),
                eq(new UserRejectedContext("이서준", "서류 미비")),
                eq(new EventCause("evt-3", "user-service")));
    }
}
