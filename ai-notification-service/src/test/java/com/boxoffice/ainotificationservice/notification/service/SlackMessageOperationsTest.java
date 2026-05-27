package com.boxoffice.ainotificationservice.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.boxoffice.ainotificationservice.config.DispatchProperties;
import com.boxoffice.ainotificationservice.notification.client.SendResult;
import com.boxoffice.ainotificationservice.notification.entity.message.NotificationStatus;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.entity.message.SlackMessage;
import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.ainotificationservice.notification.repository.SlackMessageRepository;
import com.boxoffice.common.exception.BaseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("SlackMessageOperations")
@ExtendWith(MockitoExtension.class)
class SlackMessageOperationsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 22, 10, 0);
    private static final LocalDateTime PAST = LocalDateTime.of(2026, 5, 22, 9, 0);

    @Mock
    private SlackMessageRepository repository;

    private SlackMessageOperations operations;

    @BeforeEach
    void setUp() {
        operations = new SlackMessageOperations(repository, new DispatchProperties(3));
    }

    private SlackMessage newPending() {
        return SlackMessage.direct(
                "idem-1", Recipient.user("U1"), TemplateType.USER_APPROVED, "hi");
    }

    private SlackMessage pendingWithAttempts(int attempts) {
        SlackMessage message = newPending();
        for (int i = 0; i < attempts; i++) {
            message.markAsSending(PAST);
            message.markBackToPendingForRetry();
        }
        return message;
    }

    @Nested
    @DisplayName("markSending()")
    class MarkSending {

        @Test
        @DisplayName("성공 - SENDING 상태, attemptCount 증가, 스냅샷 반환")
        void marks_sending_and_returns_snapshot() {
            // given
            UUID id = UUID.randomUUID();
            SlackMessage message = newPending();
            given(repository.findById(id)).willReturn(Optional.of(message));

            // when
            DispatchSnapshot snapshot = operations.markSending(id, NOW);

            // then
            assertThat(message.getStatus()).isEqualTo(NotificationStatus.SENDING);
            assertThat(message.getAttemptCount()).isEqualTo(1);
            assertThat(snapshot.recipient()).isEqualTo(Recipient.user("U1"));
            assertThat(snapshot.body()).isEqualTo("hi");
        }

        @Test
        @DisplayName("실패 - 메시지 없음은 NOTIFICATION_NOT_FOUND")
        void fail_message_not_found() {
            // given
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> operations.markSending(id, NOW))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("markSendingIfRetryable()")
    class MarkSendingIfRetryable {

        @Test
        @DisplayName("PENDING - SENDING 전환 + 스냅샷 반환")
        void retryable_marks_sending() {
            // given
            UUID id = UUID.randomUUID();
            SlackMessage message = pendingWithAttempts(1);
            given(repository.findById(id)).willReturn(Optional.of(message));

            // when
            Optional<DispatchSnapshot> snapshot = operations.markSendingIfRetryable(id, NOW);

            // then
            assertThat(snapshot).isPresent();
            assertThat(message.getStatus()).isEqualTo(NotificationStatus.SENDING);
            assertThat(message.getAttemptCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("PENDING이 아닌 상태 - Optional.empty, 상태 변경 X")
        void non_retryable_returns_empty() {
            // given
            UUID id = UUID.randomUUID();
            SlackMessage message = newPending();
            message.markAsSending(PAST);
            message.markAsSent(PAST);
            given(repository.findById(id)).willReturn(Optional.of(message));

            // when
            Optional<DispatchSnapshot> snapshot = operations.markSendingIfRetryable(id, NOW);

            // then
            assertThat(snapshot).isEmpty();
            assertThat(message.getStatus()).isEqualTo(NotificationStatus.SENT);
        }
    }

    @Nested
    @DisplayName("applyResult()")
    class ApplyResult {

        @Test
        @DisplayName("성공 - SENT 상태")
        void success_marks_sent() {
            // given
            UUID id = UUID.randomUUID();
            SlackMessage message = newPending();
            message.markAsSending(PAST);
            given(repository.findById(id)).willReturn(Optional.of(message));

            // when
            operations.applyResult(id, SendResult.success(200, Duration.ofMillis(10)), NOW);

            // then
            assertThat(message.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(message.sentAtOptional()).contains(NOW);
        }

        @Test
        @DisplayName("영구 실패 - FAILED")
        void permanent_failure_marks_failed() {
            // given
            UUID id = UUID.randomUUID();
            SlackMessage message = newPending();
            message.markAsSending(PAST);
            given(repository.findById(id)).willReturn(Optional.of(message));

            // when
            operations.applyResult(id,
                    SendResult.permanentFailure(404, "channel_not_found", Duration.ofMillis(5)), NOW);

            // then
            assertThat(message.getStatus()).isEqualTo(NotificationStatus.FAILED);
        }

        @Test
        @DisplayName("일시 실패 - PENDING 복귀")
        void transient_failure_marks_back_to_pending() {
            // given
            UUID id = UUID.randomUUID();
            SlackMessage message = newPending();
            message.markAsSending(PAST);
            given(repository.findById(id)).willReturn(Optional.of(message));

            // when
            operations.applyResult(id,
                    SendResult.transientFailure(503, "service_unavailable", Duration.ofMillis(12)), NOW);

            // then
            assertThat(message.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(message.getAttemptCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("일시 실패 + 시도 한도(3) 도달 → FAILED")
        void transient_failure_at_limit_marks_failed() {
            // given — 이미 2회 시도 + 이번 SENDING(3회차) 상태
            UUID id = UUID.randomUUID();
            SlackMessage message = pendingWithAttempts(2);
            message.markAsSending(PAST);
            given(repository.findById(id)).willReturn(Optional.of(message));

            // when
            operations.applyResult(id,
                    SendResult.transientFailure(503, "service_unavailable", Duration.ofMillis(12)), NOW);

            // then
            assertThat(message.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(message.getAttemptCount()).isEqualTo(3);
        }
    }
}
