package com.boxoffice.ainotificationservice.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.boxoffice.ainotificationservice.config.RecoveryProperties;
import com.boxoffice.ainotificationservice.notification.entity.message.NotificationStatus;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.entity.message.SlackMessage;
import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import com.boxoffice.ainotificationservice.notification.repository.SlackMessageRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PendingRecoveryWorker")
@ExtendWith(MockitoExtension.class)
class PendingRecoveryWorkerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-22T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private SlackMessageRepository repository;

    @Mock
    private SlackMessageDispatcher dispatcher;

    private PendingRecoveryWorker worker;

    @BeforeEach
    void setUp() {
        worker = new PendingRecoveryWorker(
                repository, dispatcher, FIXED_CLOCK,
                new RecoveryProperties(Duration.ofMinutes(5)));
    }

    private SlackMessage newPending() {
        return SlackMessage.direct(
                "idem-1", Recipient.user("U1"), TemplateType.USER_APPROVED, "hi");
    }

    @Test
    @DisplayName("stale PENDING 각각에 대해 dispatcher.retry 호출")
    void retries_each_stale_pending() {
        // given
        SlackMessage msg1 = newPending();
        SlackMessage msg2 = newPending();
        given(repository.findAllByStatusAndCreatedAtBefore(eq(NotificationStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(msg1, msg2));

        // when
        worker.recoverStalePending();

        // then
        then(dispatcher).should(times(2)).retry(nullable(UUID.class));
    }

    @Test
    @DisplayName("stale 없으면 dispatcher 호출 X")
    void no_stale_does_nothing() {
        // given
        given(repository.findAllByStatusAndCreatedAtBefore(eq(NotificationStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of());

        // when
        worker.recoverStalePending();

        // then
        then(dispatcher).should(never()).retry(any());
    }

    @Test
    @DisplayName("한 메시지 retry 중 예외 - 워커는 죽지 않고 다음 메시지 처리 계속")
    void survives_individual_retry_exception() {
        // given
        SlackMessage msg1 = newPending();
        SlackMessage msg2 = newPending();
        given(repository.findAllByStatusAndCreatedAtBefore(eq(NotificationStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(msg1, msg2));
        willThrow(new RuntimeException("dispatch failed"))
                .given(dispatcher).retry(nullable(UUID.class));

        // when & then
        assertThatCode(() -> worker.recoverStalePending()).doesNotThrowAnyException();
        then(dispatcher).should(times(2)).retry(nullable(UUID.class));
    }
}
