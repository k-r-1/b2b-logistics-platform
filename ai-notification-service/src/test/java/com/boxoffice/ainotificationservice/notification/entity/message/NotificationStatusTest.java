package com.boxoffice.ainotificationservice.notification.entity.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationStatus")
class NotificationStatusTest {

    @Nested
    @DisplayName("canTransitionTo()")
    class CanTransitionTo {

        @Test
        @DisplayName("PENDING은 SENDING/FAILED로 전이 가능")
        void pending_transitions() {
            // when & then
            assertThat(NotificationStatus.PENDING.canTransitionTo(NotificationStatus.SENDING)).isTrue();
            assertThat(NotificationStatus.PENDING.canTransitionTo(NotificationStatus.FAILED)).isTrue();
            assertThat(NotificationStatus.PENDING.canTransitionTo(NotificationStatus.SENT)).isFalse();
            assertThat(NotificationStatus.PENDING.canTransitionTo(NotificationStatus.DLQ)).isFalse();
        }

        @Test
        @DisplayName("SENDING은 SENT/FAILED/PENDING으로 전이 가능")
        void sending_transitions() {
            // when & then
            assertThat(NotificationStatus.SENDING.canTransitionTo(NotificationStatus.SENT)).isTrue();
            assertThat(NotificationStatus.SENDING.canTransitionTo(NotificationStatus.FAILED)).isTrue();
            assertThat(NotificationStatus.SENDING.canTransitionTo(NotificationStatus.PENDING)).isTrue();
            assertThat(NotificationStatus.SENDING.canTransitionTo(NotificationStatus.DLQ)).isFalse();
        }

        @Test
        @DisplayName("FAILED는 DLQ로만 전이 가능")
        void failed_to_dlq_only() {
            // when & then
            assertThat(NotificationStatus.FAILED.canTransitionTo(NotificationStatus.DLQ)).isTrue();
            assertThat(NotificationStatus.FAILED.canTransitionTo(NotificationStatus.SENT)).isFalse();
            assertThat(NotificationStatus.FAILED.canTransitionTo(NotificationStatus.PENDING)).isFalse();
        }

        @Test
        @DisplayName("SENT/DLQ는 어떤 상태로도 전이 불가")
        void terminal_no_transition() {
            // when & then
            for (NotificationStatus target : NotificationStatus.values()) {
                assertThat(NotificationStatus.SENT.canTransitionTo(target)).isFalse();
                assertThat(NotificationStatus.DLQ.canTransitionTo(target)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("isTerminal()")
    class IsTerminal {

        @Test
        @DisplayName("SENT/DLQ는 terminal")
        void terminal_states() {
            // when & then
            assertThat(NotificationStatus.SENT.isTerminal()).isTrue();
            assertThat(NotificationStatus.DLQ.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("PENDING/SENDING/FAILED는 terminal 아님")
        void non_terminal_states() {
            // when & then
            assertThat(NotificationStatus.PENDING.isTerminal()).isFalse();
            assertThat(NotificationStatus.SENDING.isTerminal()).isFalse();
            assertThat(NotificationStatus.FAILED.isTerminal()).isFalse();
        }
    }
}
