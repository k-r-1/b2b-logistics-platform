package com.boxoffice.ainotificationservice.ai.deadline;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DispatchDeadlinePrediction VO")
class DispatchDeadlinePredictionTest {

    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 5, 21, 15, 30);

    @Test
    @DisplayName("llm 팩터리 - fallbackUsed false, confidence 보존")
    void llm_factory() {
        DispatchDeadlinePrediction p = DispatchDeadlinePrediction.llm(DEADLINE, "ok", 0.9);

        assertThat(p.fallbackUsed()).isFalse();
        assertThat(p.confidenceOptional()).contains(0.9);
        assertThat(p.reasoningOptional()).contains("ok");
    }

    @Test
    @DisplayName("fallback 팩터리 - fallbackUsed true, confidence empty")
    void fallback_factory() {
        DispatchDeadlinePrediction p = DispatchDeadlinePrediction.fallback(DEADLINE);

        assertThat(p.fallbackUsed()).isTrue();
        assertThat(p.confidenceOptional()).isEmpty();
    }

    @Test
    @DisplayName("실패 - null deadline")
    void fail_null_deadline() {
        assertThatThrownBy(() -> DispatchDeadlinePrediction.llm(null, "x", 0.5))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.INVALID_PREDICTION);
    }

    @Test
    @DisplayName("실패 - confidence 범위 밖 (음수)")
    void fail_confidence_negative() {
        assertThatThrownBy(() -> DispatchDeadlinePrediction.llm(DEADLINE, "x", -0.1))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.INVALID_PREDICTION);
    }

    @Test
    @DisplayName("실패 - confidence 범위 밖 (>1)")
    void fail_confidence_over_one() {
        assertThatThrownBy(() -> DispatchDeadlinePrediction.llm(DEADLINE, "x", 1.5))
                .isInstanceOf(BaseException.class);
    }
}
