package com.boxoffice.ainotificationservice.notification.template;

// 배송 상태 전이 종류. 계약 DeliveryStatusChanged.status enum 값과 일치.
public enum DeliveryStatus {
    ARRIVED_AT_DESTINATION,
    DELIVERED,
    FAILED
}
