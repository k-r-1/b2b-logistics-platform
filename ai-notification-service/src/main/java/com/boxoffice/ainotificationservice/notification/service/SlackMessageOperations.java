package com.boxoffice.ainotificationservice.notification.service;

import com.boxoffice.ainotificationservice.config.DispatchProperties;
import com.boxoffice.ainotificationservice.notification.client.FailureType;
import com.boxoffice.ainotificationservice.notification.client.SendResult;
import com.boxoffice.ainotificationservice.notification.entity.message.SlackMessage;
import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.ainotificationservice.notification.repository.SlackMessageRepository;
import com.boxoffice.common.exception.BaseException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// SlackMessage의 트랜잭션 단위 DB 작업. Dispatcher가 외부 호출 사이에 호출.
// 모두 REQUIRES_NEW — 외부 호출과 DB 작업의 트랜잭션 경계를 명시 분리.
@Component
@RequiredArgsConstructor
public class SlackMessageOperations {

    private final SlackMessageRepository repository;
    private final DispatchProperties dispatchProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DispatchSnapshot markSending(UUID messageId, LocalDateTime now) {
        SlackMessage message = loadOrThrow(messageId);
        message.markAsSending(now);
        return new DispatchSnapshot(message.recipient(), message.getBody());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<DispatchSnapshot> markSendingIfRetryable(UUID messageId, LocalDateTime now) {
        SlackMessage message = loadOrThrow(messageId);
        if (!message.canBeRetried()) {
            return Optional.empty();
        }
        message.markAsSending(now);
        return Optional.of(new DispatchSnapshot(message.recipient(), message.getBody()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyResult(UUID messageId, SendResult result, LocalDateTime now) {
        SlackMessage message = loadOrThrow(messageId);
        if (result.success()) {
            message.markAsSent(now);
            return;
        }
        FailureType failureType = result.failureTypeOptional().orElseThrow();
        if (failureType == FailureType.PERMANENT || message.getAttemptCount() >= dispatchProperties.maxAttempts()) {
            message.markAsFailed();
        } else {
            message.markBackToPendingForRetry();
        }
    }

    private SlackMessage loadOrThrow(UUID messageId) {
        return repository.findById(messageId)
                .orElseThrow(() -> new BaseException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
