package com.boxoffice.ainotificationservice.notification.consumer;

import com.boxoffice.ainotificationservice.notification.entity.inbox.ProcessedEvent;
import com.boxoffice.ainotificationservice.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Kafka 이벤트 중복 처리 차단. (eventId, consumerGroup) 미처리 시에만 기록 후 action 실행.
// 처리 기록 저장과 action을 한 트랜잭션으로 묶어, action 실패 시 기록도 롤백되어 재시도 가능.
@Component
@RequiredArgsConstructor
public class IdempotentEventProcessor {

    private final ProcessedEventRepository repository;

    @Transactional
    public void processOnce(String eventId, String consumerGroup, Runnable action) {
        if (repository.existsByEventIdAndConsumerGroup(eventId, consumerGroup)) {
            return;
        }
        repository.save(ProcessedEvent.of(eventId, consumerGroup));
        action.run();
    }
}
