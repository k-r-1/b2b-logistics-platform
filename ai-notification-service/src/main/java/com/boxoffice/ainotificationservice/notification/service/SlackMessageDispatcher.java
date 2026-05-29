package com.boxoffice.ainotificationservice.notification.service;

import com.boxoffice.ainotificationservice.notification.client.NotificationClient;
import com.boxoffice.ainotificationservice.notification.client.SendRequest;
import com.boxoffice.ainotificationservice.notification.client.SendResult;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// PENDING 커밋 후 Slack 발송 + 결과 상태 전이.
// 트랜잭션 X — DB 작업은 SlackMessageOperations에 위임, 외부 API 호출은 트랜잭션 밖에서 수행.
@Component
@RequiredArgsConstructor
public class SlackMessageDispatcher {

    private final SlackMessageOperations operations;
    private final NotificationClient client;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueued(SlackMessageQueuedEvent event) {
        LocalDateTime now = LocalDateTime.now(clock);
        DispatchSnapshot snapshot = operations.markSending(event.messageId(), now);
        sendAndApply(event.messageId(), snapshot, now);
    }

    // 잔존 복구 워커가 호출. 재시도 불가 상태(이미 처리됨)는 skip.
    public void retry(UUID messageId) {
        LocalDateTime now = LocalDateTime.now(clock);
        operations.markSendingIfRetryable(messageId, now)
                .ifPresent(snapshot -> sendAndApply(messageId, snapshot, now));
    }

    private void sendAndApply(UUID messageId, DispatchSnapshot snapshot, LocalDateTime now) {
        SendResult result = client.send(new SendRequest(snapshot.recipient(), snapshot.body()));
        operations.applyResult(messageId, result, now);
    }
}
