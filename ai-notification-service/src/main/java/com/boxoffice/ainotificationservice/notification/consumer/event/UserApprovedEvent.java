package com.boxoffice.ainotificationservice.notification.consumer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// user.events / UserApproved 가정 페이로드. 계약 합의 전 가정이며 추후 reconcile.
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserApprovedEvent(
        String eventId,
        String userName
) implements UserEvent {

}
