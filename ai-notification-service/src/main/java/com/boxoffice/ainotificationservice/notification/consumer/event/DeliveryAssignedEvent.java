package com.boxoffice.ainotificationservice.notification.consumer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// delivery-manager.events / DeliveryAssigned 가정 페이로드. 계약 합의 전 가정이며 추후 reconcile.
// 발송 시한 예측·알림에 사용하는 필드만 정의(나머지는 무시).
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryAssignedEvent(
        String eventId,
        Order order,
        Route route,
        long totalEstimatedDurationSeconds,
        Agent agent
) implements DeliveryManagerEvent {

    @JsonIgnoreProperties(ignoreUnknown = true)
    // requestedDeadline은 ISO-8601 offset 포함 문자열(예: "2026-06-01T18:00:00+09:00").
    // 타임존 보정으로 벽시계 시각이 흔들리지 않도록 String으로 받아 컨슈머에서 파싱한다.
    public record Order(
            String orderId,
            List<Product> products,
            String requesterNote,
            String requestedDeadline
    ) {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(String name, int quantity) {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(String origin, List<String> waypoints, String destination) {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Agent(String name, WorkingHours workingHours) {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WorkingHours(String start, String end) {

    }
}
