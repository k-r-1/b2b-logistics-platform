package com.boxoffice.ainotificationservice.ai.deadline;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;

import java.time.LocalDateTime;
import java.util.Optional;

public record DispatchDeadlinePrediction(
        LocalDateTime dispatchDeadline,
        String reasoning,
        Double confidence,
        boolean fallbackUsed
) {

    public DispatchDeadlinePrediction {
        if (dispatchDeadline == null) {
            throw new BaseException(AiErrorCode.INVALID_PREDICTION);
        }
        if (confidence != null && (confidence < 0.0 || confidence > 1.0)) {
            throw new BaseException(AiErrorCode.INVALID_PREDICTION);
        }
    }

    public static DispatchDeadlinePrediction llm(LocalDateTime deadline, String reasoning, Double confidence) {
        return new DispatchDeadlinePrediction(deadline, reasoning, confidence, false);
    }

    public static DispatchDeadlinePrediction fallback(LocalDateTime deadline) {
        return new DispatchDeadlinePrediction(deadline, "rule-based fallback", null, true);
    }

    public Optional<String> reasoningOptional() {
        return Optional.ofNullable(reasoning);
    }

    public Optional<Double> confidenceOptional() {
        return Optional.ofNullable(confidence);
    }
}
