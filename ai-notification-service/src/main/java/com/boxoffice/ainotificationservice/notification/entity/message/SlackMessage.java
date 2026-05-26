package com.boxoffice.ainotificationservice.notification.entity.message;

import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.entity.BaseEntity;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

// Slack 메시지 본체. 상태 전이·멱등성·원인 이벤트·발송 시도 횟수를 추적.
// Recipient/EventCause VO는 컬럼으로 분해 저장(JPA + record @Embeddable 제약).
@Getter
@Entity
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "slack_message",
        schema = "notification_schema",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_slack_message_idempotency_key",
                columnNames = "idempotency_key"
        )
)
public class SlackMessage extends BaseEntity {

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "recipient_id", nullable = false, length = 100)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 16)
    private RecipientType recipientType;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 64)
    private TemplateType templateType;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "cause_event_id", length = 128)
    private String causeEventId;

    @Column(name = "cause_source", length = 64)
    private String causeSource;

    private SlackMessage(String idempotencyKey,
            Recipient recipient,
            TemplateType templateType,
            String body,
            EventCause cause) {
        validate(idempotencyKey, recipient, templateType, body);
        this.idempotencyKey = idempotencyKey;
        this.recipientId = recipient.id();
        this.recipientType = recipient.type();
        this.templateType = templateType;
        this.body = body;
        if (cause != null) {
            this.causeEventId = cause.eventId();
            this.causeSource = cause.source();
        }
        this.status = NotificationStatus.PENDING;
        this.attemptCount = 0;
    }

    // 직접 발송 API
    public static SlackMessage direct(String idempotencyKey,
            Recipient recipient,
            TemplateType templateType,
            String body) {
        return new SlackMessage(idempotencyKey, recipient, templateType, body, null);
    }

    // Kafka/외부 이벤트 트리거
    public static SlackMessage fromEvent(String idempotencyKey,
            Recipient recipient,
            TemplateType templateType,
            String body,
            EventCause cause) {
        if (cause == null) {
            throw new BaseException(NotificationErrorCode.INVALID_EVENT_CAUSE);
        }
        return new SlackMessage(idempotencyKey, recipient, templateType, body, cause);
    }

    private static void requireNonNullTime(LocalDateTime time) {
        if (time == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private static void validate(String idempotencyKey, Recipient recipient, TemplateType templateType, String body) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BaseException(NotificationErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
        if (recipient == null) {
            throw new BaseException(NotificationErrorCode.INVALID_RECIPIENT);
        }
        if (templateType == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
        if (body == null || body.isBlank()) {
            throw new BaseException(NotificationErrorCode.INVALID_MESSAGE_BODY);
        }
    }

    public void markAsSending(LocalDateTime now) {
        requireNonNullTime(now);
        transitionTo(NotificationStatus.SENDING);
        this.attemptCount += 1;
        this.lastAttemptedAt = now;
    }

    public void markAsSent(LocalDateTime sentAt) {
        requireNonNullTime(sentAt);
        transitionTo(NotificationStatus.SENT);
        this.sentAt = sentAt;
    }

    public void markAsFailed() {
        transitionTo(NotificationStatus.FAILED);
    }

    public void markBackToPendingForRetry() {
        transitionTo(NotificationStatus.PENDING);
    }

    public void moveToDlq() {
        transitionTo(NotificationStatus.DLQ);
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }

    public boolean canBeRetried() {
        return status == NotificationStatus.PENDING;
    }

    public Recipient recipient() {
        return new Recipient(recipientId, recipientType);
    }

    public Optional<EventCause> cause() {
        return EventCause.optional(causeEventId, causeSource);
    }

    public Optional<LocalDateTime> sentAtOptional() {
        return Optional.ofNullable(sentAt);
    }

    public Optional<LocalDateTime> lastAttemptedAtOptional() {
        return Optional.ofNullable(lastAttemptedAt);
    }

    private void transitionTo(NotificationStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new BaseException(NotificationErrorCode.INVALID_STATE_TRANSITION);
        }
        this.status = next;
    }
}
