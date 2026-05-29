package com.boxoffice.ainotificationservice.ai.deadline;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record WorkingHours(LocalTime start, LocalTime end) {

    public WorkingHours {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BaseException(AiErrorCode.INVALID_WORKING_HOURS);
        }
    }

    public static WorkingHours defaultHours() {
        return new WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0));
    }

    // 납기 역산 결과를 근무시간 안으로 정규화: 종료 후 → 당일 종료, 시작 전 → 전날 종료.
    public LocalDateTime adjustToWithin(LocalDateTime candidate) {
        if (candidate == null) {
            throw new BaseException(AiErrorCode.INVALID_DISPATCH_INPUT);
        }
        LocalTime time = candidate.toLocalTime();
        if (!time.isBefore(end)) {
            return candidate.toLocalDate().atTime(end);
        }
        if (time.isBefore(start)) {
            return candidate.toLocalDate().minusDays(1).atTime(end);
        }
        return candidate;
    }
}
