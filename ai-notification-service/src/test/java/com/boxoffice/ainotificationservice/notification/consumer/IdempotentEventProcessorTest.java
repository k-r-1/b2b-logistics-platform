package com.boxoffice.ainotificationservice.notification.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.boxoffice.ainotificationservice.notification.entity.inbox.ProcessedEvent;
import com.boxoffice.ainotificationservice.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("IdempotentEventProcessor")
@ExtendWith(MockitoExtension.class)
class IdempotentEventProcessorTest {

    private static final String EVENT_ID = "evt-1";
    private static final String GROUP = "ai-notification-service";

    @Mock
    private ProcessedEventRepository repository;

    @Mock
    private Runnable action;

    private IdempotentEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new IdempotentEventProcessor(repository);
    }

    @Test
    @DisplayName("최초 처리 - 처리 기록 저장 + action 실행")
    void first_time_saves_and_runs() {
        // given
        given(repository.existsByEventIdAndConsumerGroup(EVENT_ID, GROUP)).willReturn(false);

        // when
        processor.processOnce(EVENT_ID, GROUP, action);

        // then
        then(repository).should().save(any(ProcessedEvent.class));
        then(action).should().run();
    }

    @Test
    @DisplayName("중복 처리 - 이미 기록되어 있으면 저장/action 모두 skip")
    void duplicate_skips_save_and_action() {
        // given
        given(repository.existsByEventIdAndConsumerGroup(EVENT_ID, GROUP)).willReturn(true);

        // when
        processor.processOnce(EVENT_ID, GROUP, action);

        // then
        then(repository).should(never()).save(any());
        then(action).should(never()).run();
    }
}
