package com.boxoffice.ainotificationservice.notification.entity.inbox;

import java.io.Serializable;

public record ProcessedEventId(String eventId, String consumerGroup) implements Serializable {

}
