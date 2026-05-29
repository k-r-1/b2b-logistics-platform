package com.boxoffice.ainotificationservice.ai.deadline;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;

import java.util.List;

// 발송지·경유지·도착지. 좌표는 발송 시한 계산에 불필요해 미포함(경로 정렬 기능에서 별도 도입).
public record DeliveryRoute(String origin, List<String> waypoints, String destination) {

    public DeliveryRoute {
        if (origin == null || origin.isBlank()
                || destination == null || destination.isBlank()
                || waypoints == null) {
            throw new BaseException(AiErrorCode.INVALID_DISPATCH_INPUT);
        }
        waypoints = List.copyOf(waypoints);
    }
}
