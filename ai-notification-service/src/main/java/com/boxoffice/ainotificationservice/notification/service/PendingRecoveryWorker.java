package com.boxoffice.ainotificationservice.notification.service;

import com.boxoffice.ainotificationservice.config.RecoveryProperties;
import com.boxoffice.ainotificationservice.notification.entity.message.NotificationStatus;
import com.boxoffice.ainotificationservice.notification.entity.message.SlackMessage;
import com.boxoffice.ainotificationservice.notification.repository.SlackMessageRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// PENDING 잔존 복구 — 트랜잭션 2 실패/앱 크래시로 PENDING에 머문 메시지를 주기적으로 재시도.
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingRecoveryWorker {

    private final SlackMessageRepository repository;
    private final SlackMessageDispatcher dispatcher;
    private final Clock clock;
    private final RecoveryProperties properties;

    @Scheduled(fixedDelayString = "${notification.recovery.interval-ms:60000}")
    public void recoverStalePending() {
        LocalDateTime threshold = LocalDateTime.now(clock).minus(properties.staleAfter());
        List<SlackMessage> stale = repository.findAllByStatusAndCreatedAtBefore(
                NotificationStatus.PENDING, threshold);
        stale.forEach(message -> retrySafely(message.getId()));
    }

    private void retrySafely(UUID messageId) {
        try {
            dispatcher.retry(messageId);
        } catch (Exception e) {
            log.warn("Failed to retry stale message {}: {}", messageId, e.getMessage());
        }
    }
}
