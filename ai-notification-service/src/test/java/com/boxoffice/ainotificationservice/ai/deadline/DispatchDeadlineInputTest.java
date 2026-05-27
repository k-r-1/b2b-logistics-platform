package com.boxoffice.ainotificationservice.ai.deadline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DispatchDeadlineInput VO")
class DispatchDeadlineInputTest {

    private static final LocalDateTime REQUESTED_DEADLINE = LocalDateTime.of(2026, 5, 21, 18, 0);
    private static final Duration ESTIMATED_DURATION = Duration.ofMinutes(120);

    private final WorkingHours hours = WorkingHours.defaultHours();

    @Nested
    @DisplayName("생성자 검증")
    class Construction {

        @Test
        @DisplayName("성공 - 유효한 입력")
        void success() {
            // when
            DispatchDeadlineInput input = new DispatchDeadlineInput(REQUESTED_DEADLINE, ESTIMATED_DURATION, hours);

            // then
            assertThat(input.requestedDeadline()).isEqualTo(REQUESTED_DEADLINE);
            assertThat(input.totalEstimatedDuration()).isEqualTo(ESTIMATED_DURATION);
            assertThat(input.agentWorkingHours()).isEqualTo(hours);
        }

        @Test
        @DisplayName("실패 - requestedDeadline null")
        void fail_null_deadline() {
            // when & then
            assertThatThrownBy(() -> new DispatchDeadlineInput(null, ESTIMATED_DURATION, hours))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }

        @Test
        @DisplayName("실패 - totalEstimatedDuration null")
        void fail_null_duration() {
            // when & then
            assertThatThrownBy(() -> new DispatchDeadlineInput(REQUESTED_DEADLINE, null, hours))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }

        @Test
        @DisplayName("실패 - totalEstimatedDuration 음수")
        void fail_negative_duration() {
            // given
            Duration negative = Duration.ofMinutes(-1);

            // when & then
            assertThatThrownBy(() -> new DispatchDeadlineInput(REQUESTED_DEADLINE, negative, hours))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }

        @Test
        @DisplayName("실패 - agentWorkingHours null")
        void fail_null_working_hours() {
            // when & then
            assertThatThrownBy(() -> new DispatchDeadlineInput(REQUESTED_DEADLINE, ESTIMATED_DURATION, null))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }
    }
}
