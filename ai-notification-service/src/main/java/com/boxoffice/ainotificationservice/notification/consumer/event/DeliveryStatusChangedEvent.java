package com.boxoffice.ainotificationservice.notification.consumer.event;

import com.boxoffice.ainotificationservice.notification.template.DeliveryStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// delivery.events / DeliveryStatusChanged 가정 페이로드. 계약 합의 전 가정이며 추후 reconcile.
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryStatusChangedEvent(
        String eventId,
        String deliveryId,
        String orderId,
        DeliveryStatus status,
        String recipientName,
        String failureReason
) implements DeliveryEvent {

}
