package com.boxoffice.ainotificationservice.notification.consumer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// user.events / UserSignupRequested 가정 페이로드. 계약 합의 전 가정이며 추후 reconcile.
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSignupRequestedEvent(
        String eventId,
        String applicantName,
        String email,
        String requestedRole
) implements UserEvent {

}
