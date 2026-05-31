package com.boxoffice.ainotificationservice.notification.consumer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// user.events 토픽 이벤트. eventType 필드로 구체 타입을 결정(다형성 역직렬화).
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserSignupRequestedEvent.class, name = "UserSignupRequested"),
        @JsonSubTypes.Type(value = UserApprovedEvent.class, name = "UserApproved"),
        @JsonSubTypes.Type(value = UserRejectedEvent.class, name = "UserRejected")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public sealed interface UserEvent
        permits UserSignupRequestedEvent, UserApprovedEvent, UserRejectedEvent {

    String eventId();
}
