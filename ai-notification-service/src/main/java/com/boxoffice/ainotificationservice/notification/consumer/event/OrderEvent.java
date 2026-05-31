package com.boxoffice.ainotificationservice.notification.consumer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// order.events 토픽 이벤트. eventType 필드로 구체 타입을 결정(다형성 역직렬화).
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderCanceledEvent.class, name = "OrderCanceled")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public sealed interface OrderEvent permits OrderCanceledEvent {

    String eventId();
}
