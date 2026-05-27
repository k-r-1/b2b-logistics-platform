package com.boxoffice.ainotificationservice.ai.deadline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DispatchDeadlinePrediction VO")
class DispatchDeadlinePredictionTest {

    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 5, 21, 15, 30);

    @Nested
    @DisplayName("정적 팩터리")
    class Factory {

        @Test
        @DisplayName("llm - fallbackUsed false, confidence·reasoning 보존")
        void llm_factory() {
            // when
            DispatchDeadlinePrediction prediction = DispatchDeadlinePrediction.llm(DEADLINE, "ok", 0.9);

            // then
            assertThat(prediction.fallbackUsed()).isFalse();
            assertThat(prediction.confidenceOptional()).contains(0.9);
            assertThat(prediction.reasoningOptional()).contains("ok");
        }

        @Test
        @DisplayName("fallback - fallbackUsed true, confidence empty")
        void fallback_factory() {
            // when
            DispatchDeadlinePrediction prediction = DispatchDeadlinePrediction.fallback(DEADLINE);

            // then
            assertThat(prediction.fallbackUsed()).isTrue();
            assertThat(prediction.confidenceOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("검증")
    class Validation {

        @Test
        @DisplayName("실패 - null deadline")
        void fail_null_deadline() {
            // when & then
            assertThatThrownBy(() -> DispatchDeadlinePrediction.llm(null, "x", 0.5))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_PREDICTION);
        }

        @Test
        @DisplayName("실패 - confidence 음수")
        void fail_confidence_negative() {
            // when & then
            assertThatThrownBy(() -> DispatchDeadlinePrediction.llm(DEADLINE, "x", -0.1))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_PREDICTION);
        }

        @Test
        @DisplayName("실패 - confidence > 1")
        void fail_confidence_over_one() {
            // when & then
            assertThatThrownBy(() -> DispatchDeadlinePrediction.llm(DEADLINE, "x", 1.5))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_PREDICTION);
        }
    }
}
