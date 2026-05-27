package com.boxoffice.ainotificationservice.notification.entity.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SlackMessage 도메인")
class SlackMessageTest {

    private static final LocalDateTime ATTEMPTED_AT = LocalDateTime.of(2026, 5, 20, 10, 0);
    private static final LocalDateTime SENT_AT = ATTEMPTED_AT.plusMinutes(1);
    private static final LocalDateTime RETRIED_AT = ATTEMPTED_AT.plusMinutes(2);

    private SlackMessage newPending() {
        return SlackMessage.direct(
                "idem-1",
                Recipient.user("U12345"),
                TemplateType.USER_APPROVED,
                "안녕하세요"
        );
    }

    @Nested
    @DisplayName("direct()")
    class Direct {

        @Test
        @DisplayName("성공 - PENDING, attemptCount 0, cause empty, Optional 필드 empty")
        void success_initial_state() {
            // when
            SlackMessage msg = newPending();

            // then
            assertThat(msg.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(msg.getAttemptCount()).isZero();
            assertThat(msg.sentAtOptional()).isEmpty();
            assertThat(msg.lastAttemptedAtOptional()).isEmpty();
            assertThat(msg.cause()).isEmpty();
            assertThat(msg.recipient()).isEqualTo(Recipient.user("U12345"));
        }

        @Test
        @DisplayName("실패 - 빈 idempotencyKey")
        void fail_blank_idempotency_key() {
            // given
            Recipient recipient = Recipient.user("U1");

            // when & then
            assertThatThrownBy(() -> SlackMessage.direct(
                    "  ", recipient, TemplateType.USER_APPROVED, "hi"
            ))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_IDEMPOTENCY_KEY);
        }

        @Test
        @DisplayName("실패 - null recipient")
        void fail_null_recipient() {
            // when & then
            assertThatThrownBy(() -> SlackMessage.direct(
                    "idem-1", null, TemplateType.USER_APPROVED, "hi"
            ))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_RECIPIENT);
        }

        @Test
        @DisplayName("실패 - 빈 body")
        void fail_blank_body() {
            // given
            Recipient recipient = Recipient.user("U1");

            // when & then
            assertThatThrownBy(() -> SlackMessage.direct(
                    "idem-1", recipient, TemplateType.USER_APPROVED, " "
            ))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_MESSAGE_BODY);
        }
    }

    @Nested
    @DisplayName("fromEvent()")
    class FromEvent {

        @Test
        @DisplayName("성공 - cause 보존")
        void with_cause() {
            // when
            SlackMessage msg = SlackMessage.fromEvent(
                    "idem-x",
                    Recipient.channel("C1"),
                    TemplateType.ORDER_CANCELED,
                    "주문 취소",
                    new EventCause("evt-100", "order-service")
            );

            // then
            assertThat(msg.cause()).contains(new EventCause("evt-100", "order-service"));
        }

        @Test
        @DisplayName("실패 - null cause는 INVALID_EVENT_CAUSE")
        void fail_null_cause() {
            // given
            Recipient recipient = Recipient.channel("C1");

            // when & then
            //noinspection DataFlowIssue
            assertThatThrownBy(() -> SlackMessage.fromEvent(
                    "idem-x", recipient, TemplateType.ORDER_CANCELED, "본문", null
            ))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_EVENT_CAUSE);
        }
    }

    @Nested
    @DisplayName("markAsSending()")
    class MarkAsSending {

        @Test
        @DisplayName("성공 - PENDING → SENDING, attemptCount +1, lastAttemptedAt 세팅")
        void success_pending_to_sending() {
            // given
            SlackMessage msg = newPending();

            // when
            msg.markAsSending(ATTEMPTED_AT);

            // then
            assertThat(msg.getStatus()).isEqualTo(NotificationStatus.SENDING);
            assertThat(msg.getAttemptCount()).isEqualTo(1);
            assertThat(msg.lastAttemptedAtOptional()).contains(ATTEMPTED_AT);
        }

        @Test
        @DisplayName("실패 - SENDING 중에 다시 sending")
        void fail_already_sending() {
            // given
            SlackMessage msg = newPending();
            msg.markAsSending(ATTEMPTED_AT);

            // when & then
            assertThatThrownBy(() -> msg.markAsSending(SENT_AT))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_STATE_TRANSITION);
        }

        @Test
        @DisplayName("실패 - null 시각")
        void fail_null_time() {
            // given
            SlackMessage msg = newPending();

            // when & then
            assertThatThrownBy(() -> msg.markAsSending(null))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_INPUT);
        }
    }

    @Nested
    @DisplayName("markAsSent()")
    class MarkAsSent {

        @Test
        @DisplayName("성공 - SENDING → SENT, sentAt 세팅")
        void success_sending_to_sent() {
            // given
            SlackMessage msg = newPending();
            msg.markAsSending(ATTEMPTED_AT);

            // when
            msg.markAsSent(SENT_AT);

            // then
            assertThat(msg.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(msg.sentAtOptional()).contains(SENT_AT);
        }

        @Test
        @DisplayName("실패 - PENDING 상태에서 markAsSent")
        void fail_from_pending() {
            // given
            SlackMessage msg = newPending();

            // when & then
            assertThatThrownBy(() -> msg.markAsSent(ATTEMPTED_AT))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_STATE_TRANSITION);
        }

        @Test
        @DisplayName("실패 - null 시각")
        void fail_null_time() {
            // given
            SlackMessage msg = newPending();
            msg.markAsSending(ATTEMPTED_AT);

            // when & then
            assertThatThrownBy(() -> msg.markAsSent(null))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_INPUT);
        }
    }

    @Nested
    @DisplayName("markAsFailed()")
    class MarkAsFailed {

        @Test
        @DisplayName("성공 - SENDING → FAILED")
        void success_from_sending() {
            // given
            SlackMessage msg = newPending();
            msg.markAsSending(ATTEMPTED_AT);

            // when
            msg.markAsFailed();

            // then
            assertThat(msg.getStatus()).isEqualTo(NotificationStatus.FAILED);
        }

        @Test
        @DisplayName("성공 - PENDING → FAILED (잔존 복구 임계치 초과)")
        void success_from_pending() {
            // given
            SlackMessage msg = newPending();

            // when
            msg.markAsFailed();

            // then
            assertThat(msg.getStatus()).isEqualTo(NotificationStatus.FAILED);
        }

        @Test
        @DisplayName("실패 - SENT 상태에서 markAsFailed")
        void fail_from_sent() {
            // given
            SlackMessage msg = newPending();
            msg.markAsSending(ATTEMPTED_AT);
            msg.markAsSent(SENT_AT);

            // when & then
            assertThatThrownBy(msg::markAsFailed)
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_STATE_TRANSITION);
        }
    }

    @Nested
    @DisplayName("markBackToPendingForRetry()")
    class MarkBackToPending {

        @Test
        @DisplayName("성공 - SENDING → PENDING")
        void success_back_to_pending() {
            // given
            SlackMessage msg = newPending();
            msg.markAsSending(ATTEMPTED_AT);

            // when
            msg.markBackToPendingForRetry();

            // then
            assertThat(msg.getStatus()).isEqualTo(NotificationStatus.PENDING);
        }

        @Test
        @DisplayName("재시도 누적 - sending → retry → sending → attemptCount 2")
        void retry_accumulates_attempt_count() {
            // given
            SlackMessage msg = newPending();

            // when
            msg.markAsSending(ATTEMPTED_AT);
            msg.markBackToPendingForRetry();
            msg.markAsSending(RETRIED_AT);

            // then
            assertThat(msg.getAttemptCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("moveToDlq()")
    class MoveToDlq {

        @Test
        @DisplayName("성공 - FAILED → DLQ")
        void success_from_failed() {
            // given
            SlackMessage msg = newPending();
            msg.markAsSending(ATTEMPTED_AT);
            msg.markAsFailed();

            // when
            msg.moveToDlq();

            // then
            assertThat(msg.getStatus()).isEqualTo(NotificationStatus.DLQ);
        }

        @Test
        @DisplayName("실패 - SENT 상태에서 moveToDlq")
        void fail_from_sent() {
            // given
            SlackMessage msg = newPending();
            msg.markAsSending(ATTEMPTED_AT);
            msg.markAsSent(SENT_AT);

            // when & then
            assertThatThrownBy(msg::moveToDlq)
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_STATE_TRANSITION);
        }
    }

    @Nested
    @DisplayName("isTerminal()")
    class IsTerminal {

        @Test
        @DisplayName("SENT 상태는 terminal")
        void terminal_when_sent() {
            // given
            SlackMessage msg = newPending();
            msg.markAsSending(ATTEMPTED_AT);
            msg.markAsSent(SENT_AT);

            // when & then
            assertThat(msg.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("DLQ 상태는 terminal")
        void terminal_when_dlq() {
            // given
            SlackMessage msg = newPending();
            msg.markAsSending(ATTEMPTED_AT);
            msg.markAsFailed();
            msg.moveToDlq();

            // when & then
            assertThat(msg.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("PENDING/SENDING/FAILED는 terminal 아님")
        void not_terminal_for_active_states() {
            // given
            SlackMessage pending = newPending();
            SlackMessage sending = newPending();
            sending.markAsSending(ATTEMPTED_AT);
            SlackMessage failed = newPending();
            failed.markAsFailed();

            // when & then
            assertThat(pending.isTerminal()).isFalse();
            assertThat(sending.isTerminal()).isFalse();
            assertThat(failed.isTerminal()).isFalse();
        }
    }
}
