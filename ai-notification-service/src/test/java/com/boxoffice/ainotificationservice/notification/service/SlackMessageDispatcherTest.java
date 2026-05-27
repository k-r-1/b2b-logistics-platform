package com.boxoffice.ainotificationservice.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.boxoffice.ainotificationservice.notification.client.NotificationClient;
import com.boxoffice.ainotificationservice.notification.client.SendRequest;
import com.boxoffice.ainotificationservice.notification.client.SendResult;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("SlackMessageDispatcher")
@ExtendWith(MockitoExtension.class)
class SlackMessageDispatcherTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-22T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime EXPECTED_NOW = LocalDateTime.ofInstant(
            FIXED_CLOCK.instant(), ZoneOffset.UTC);

    @Mock
    private SlackMessageOperations operations;

    @Mock
    private NotificationClient client;

    private SlackMessageDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new SlackMessageDispatcher(operations, client, FIXED_CLOCK);
    }

    @Nested
    @DisplayName("onQueued()")
    class OnQueued {

        @Test
        @DisplayName("markSending → client.send → applyResult 순으로 호출, 외부 호출은 트랜잭션 밖")
        void invokes_operations_and_client_in_order() {
            // given
            UUID id = UUID.randomUUID();
            Recipient recipient = Recipient.user("U1");
            DispatchSnapshot snapshot = new DispatchSnapshot(recipient, "hi");
            SendResult result = SendResult.success(200, Duration.ofMillis(10));
            given(operations.markSending(id, EXPECTED_NOW)).willReturn(snapshot);
            given(client.send(new SendRequest(recipient, "hi"))).willReturn(result);

            // when
            dispatcher.onQueued(new SlackMessageQueuedEvent(id, "hi", recipient));

            // then — 순서 검증으로 외부 호출이 markSending과 applyResult 사이임을 보장
            InOrder order = inOrder(operations, client);
            order.verify(operations).markSending(id, EXPECTED_NOW);
            order.verify(client).send(new SendRequest(recipient, "hi"));
            order.verify(operations).applyResult(id, result, EXPECTED_NOW);
        }
    }

    @Nested
    @DisplayName("retry()")
    class Retry {

        @Test
        @DisplayName("재시도 가능 - markSendingIfRetryable → client.send → applyResult")
        void retryable_dispatches() {
            // given
            UUID id = UUID.randomUUID();
            Recipient recipient = Recipient.user("U1");
            DispatchSnapshot snapshot = new DispatchSnapshot(recipient, "hi");
            SendResult result = SendResult.success(200, Duration.ofMillis(5));
            given(operations.markSendingIfRetryable(id, EXPECTED_NOW)).willReturn(Optional.of(snapshot));
            given(client.send(any(SendRequest.class))).willReturn(result);

            // when
            dispatcher.retry(id);

            // then
            InOrder order = inOrder(operations, client);
            order.verify(operations).markSendingIfRetryable(id, EXPECTED_NOW);
            order.verify(client).send(new SendRequest(recipient, "hi"));
            order.verify(operations).applyResult(id, result, EXPECTED_NOW);
        }

        @Test
        @DisplayName("재시도 불가 - client.send/applyResult 호출 X")
        void non_retryable_skips() {
            // given
            UUID id = UUID.randomUUID();
            given(operations.markSendingIfRetryable(id, EXPECTED_NOW)).willReturn(Optional.empty());

            // when
            dispatcher.retry(id);

            // then
            then(client).should(never()).send(any());
            then(operations).should(never()).applyResult(eq(id), any(), any());
        }
    }
}
