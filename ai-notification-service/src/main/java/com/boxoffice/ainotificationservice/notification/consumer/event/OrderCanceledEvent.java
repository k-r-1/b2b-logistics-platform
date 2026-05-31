package com.boxoffice.ainotificationservice.notification.consumer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// order.events / OrderCanceled 가정 페이로드. 계약 합의 전 가정이며 추후 reconcile.
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCanceledEvent(
        String eventId,
        String orderId,
        String reason,
        String ordererName,
        String hubManagerName
) implements OrderEvent {

}
