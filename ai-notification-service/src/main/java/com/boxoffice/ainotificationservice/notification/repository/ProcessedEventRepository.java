package com.boxoffice.ainotificationservice.notification.repository;

import com.boxoffice.ainotificationservice.notification.entity.inbox.ProcessedEvent;
import com.boxoffice.ainotificationservice.notification.entity.inbox.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);
}
