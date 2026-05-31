package com.boxoffice.ainotificationservice.notification.consumer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// delivery.events 토픽 이벤트. eventType 필드로 구체 타입을 결정(다형성 역직렬화).
// DailyDeliveryScheduleAssigned(도전 기능)는 아직 미등록 — 수신 시 역직렬화 단계에서 DLQ로 격리된다.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeliveryStatusChangedEvent.class, name = "DeliveryStatusChanged")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public sealed interface DeliveryEvent permits DeliveryStatusChangedEvent {

    String eventId();
}
