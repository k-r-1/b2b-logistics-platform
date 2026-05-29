package com.boxoffice.ainotificationservice.ai.deadline;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;

import java.time.Duration;
import java.time.LocalDateTime;

// 규칙 기반 fallback 계산용 입력. LLM 확장 입력(상품·요청사항·경로)은 별도 record로 분리.
public record DispatchDeadlineInput(
        LocalDateTime requestedDeadline,
        Duration totalEstimatedDuration,
        WorkingHours agentWorkingHours
) {

    public DispatchDeadlineInput {
        if (requestedDeadline == null
                || totalEstimatedDuration == null || totalEstimatedDuration.isNegative()
                || agentWorkingHours == null) {
            throw new BaseException(AiErrorCode.INVALID_DISPATCH_INPUT);
        }
    }
}
