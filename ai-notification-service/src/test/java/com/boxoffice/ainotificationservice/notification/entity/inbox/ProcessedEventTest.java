package com.boxoffice.ainotificationservice.notification.entity.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProcessedEvent")
class ProcessedEventTest {

    @Nested
    @DisplayName("of()")
    class Of {

        @Test
        @DisplayName("성공 - eventId/consumerGroup 보존")
        void success() {
            // when
            ProcessedEvent ev = ProcessedEvent.of("evt-1", "notification-user-events");

            // then
            assertThat(ev.getEventId()).isEqualTo("evt-1");
            assertThat(ev.getConsumerGroup()).isEqualTo("notification-user-events");
        }

        @Test
        @DisplayName("실패 - null eventId")
        void fail_null_event_id() {
            // when & then
            assertThatThrownBy(() -> ProcessedEvent.of(null, "g"))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("실패 - 빈 consumerGroup")
        void fail_blank_consumer_group() {
            // when & then
            assertThatThrownBy(() -> ProcessedEvent.of("evt-1", "  "))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_INPUT);
        }
    }
}
