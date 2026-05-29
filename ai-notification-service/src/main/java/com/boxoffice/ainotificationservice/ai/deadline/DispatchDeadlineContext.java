package com.boxoffice.ainotificationservice.ai.deadline;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// LLM 발송 시한 예측 입력. DeliveryCreated 이벤트에서 구성(트리거 연결은 별도 작업).
// LLM 실패 시 사용할 fallback 입력은 toFallbackInput()으로 추출.
public record DispatchDeadlineContext(
        LocalDateTime requestedDeadline,
        String requesterNote,
        List<OrderLine> products,
        DeliveryRoute route,
        Duration totalEstimatedDuration,
        WorkingHours agentWorkingHours
) {

    public DispatchDeadlineContext {
        if (requestedDeadline == null
                || totalEstimatedDuration == null || totalEstimatedDuration.isNegative()
                || agentWorkingHours == null
                || route == null
                || products == null || products.isEmpty()) {
            throw new BaseException(AiErrorCode.INVALID_DISPATCH_INPUT);
        }
        products = List.copyOf(products);
    }

    public DispatchDeadlineInput toFallbackInput() {
        return new DispatchDeadlineInput(requestedDeadline, totalEstimatedDuration, agentWorkingHours);
    }

    public Optional<String> requesterNoteOptional() {
        return Optional.ofNullable(requesterNote);
    }
}
