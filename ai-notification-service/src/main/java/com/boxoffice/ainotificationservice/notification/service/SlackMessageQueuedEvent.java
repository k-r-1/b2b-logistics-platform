package com.boxoffice.ainotificationservice.notification.service;

import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import java.util.UUID;

// 트랜잭션 커밋 후 dispatcher가 Slack 발송하도록 전달하는 도메인 이벤트.
public record SlackMessageQueuedEvent(UUID messageId, String body, Recipient recipient) {

}
