package com.boxoffice.ainotificationservice.ai.service;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineInput;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

// 안전 마진 절댓값 미정 — 보수적 30분 기본, 추후 외부화 검토.
@Component
public class RuleBasedDispatchDeadlineCalculator {

    private static final Duration SAFETY_MARGIN = Duration.ofMinutes(30);

    public LocalDateTime calculate(DispatchDeadlineInput input) {
        LocalDateTime raw = input.requestedDeadline()
                .minus(input.totalEstimatedDuration())
                .minus(SAFETY_MARGIN);
        return input.agentWorkingHours().adjustToWithin(raw);
    }
}
