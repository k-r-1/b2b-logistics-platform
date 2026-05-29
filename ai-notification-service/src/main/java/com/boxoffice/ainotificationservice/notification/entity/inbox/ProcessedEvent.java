package com.boxoffice.ainotificationservice.notification.entity.inbox;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Kafka 이벤트 중복 처리 차단. 자연 키 (event_id, consumer_group)이 PK. INSERT-only.
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(ProcessedEventId.class)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "processed_event", schema = "notification_schema")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 128)
    private String eventId;

    @Id
    @Column(name = "consumer_group", length = 128)
    private String consumerGroup;

    @CreatedDate
    @Column(name = "processed_at", updatable = false, nullable = false)
    private LocalDateTime processedAt;

    private ProcessedEvent(String eventId, String consumerGroup) {
        if (eventId == null || eventId.isBlank() || consumerGroup == null || consumerGroup.isBlank()) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
        this.eventId = eventId;
        this.consumerGroup = consumerGroup;
    }

    public static ProcessedEvent of(String eventId, String consumerGroup) {
        return new ProcessedEvent(eventId, consumerGroup);
    }
}
