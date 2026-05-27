package com.boxoffice.ainotificationservice.ai.entity.prediction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PredictionLog 엔티티")
class PredictionLogTest {

    private static final String INPUT_HASH = "a".repeat(64);
    private static final LocalDateTime DISPATCH_DEADLINE = LocalDateTime.of(2026, 5, 21, 15, 30);

    @Nested
    @DisplayName("of()")
    class Of {

        @Test
        @DisplayName("성공 - LLM 예측 매핑")
        void success_llm() {
            // given
            DispatchDeadlinePrediction prediction =
                    DispatchDeadlinePrediction.llm(DISPATCH_DEADLINE, "이동시간 고려", 0.8);

            // when
            PredictionLog log = PredictionLog.of(INPUT_HASH, prediction);

            // then
            assertThat(log.getInputHash()).isEqualTo(INPUT_HASH);
            assertThat(log.getDispatchDeadline()).isEqualTo(DISPATCH_DEADLINE);
            assertThat(log.isFallbackUsed()).isFalse();
            assertThat(log.reasoningOptional()).contains("이동시간 고려");
            assertThat(log.confidenceOptional()).contains(0.8);
        }

        @Test
        @DisplayName("성공 - fallback 예측은 confidence 없음")
        void success_fallback() {
            // given
            DispatchDeadlinePrediction prediction = DispatchDeadlinePrediction.fallback(DISPATCH_DEADLINE);

            // when
            PredictionLog log = PredictionLog.of(INPUT_HASH, prediction);

            // then
            assertThat(log.isFallbackUsed()).isTrue();
            assertThat(log.confidenceOptional()).isEmpty();
        }

        @Test
        @DisplayName("실패 - inputHash blank")
        void fail_blank_hash() {
            // given
            DispatchDeadlinePrediction prediction = DispatchDeadlinePrediction.fallback(DISPATCH_DEADLINE);

            // when & then
            assertThatThrownBy(() -> PredictionLog.of(" ", prediction))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("실패 - prediction null")
        void fail_null_prediction() {
            // when & then
            assertThatThrownBy(() -> PredictionLog.of(INPUT_HASH, null))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_INPUT);
        }
    }
}
