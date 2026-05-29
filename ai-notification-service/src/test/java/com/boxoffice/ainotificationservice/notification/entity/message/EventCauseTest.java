package com.boxoffice.ainotificationservice.notification.entity.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EventCause VO")
class EventCauseTest {

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Test
        @DisplayName("성공 - 정상 생성")
        void success_create() {
            // when
            EventCause cause = new EventCause("evt-1", "order-service");

            // then
            assertThat(cause.eventId()).isEqualTo("evt-1");
            assertThat(cause.source()).isEqualTo("order-service");
        }

        @Test
        @DisplayName("실패 - blank source")
        void fail_blank_source() {
            // when & then
            assertThatThrownBy(() -> new EventCause("evt-1", "  "))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_EVENT_CAUSE);
        }
    }

    @Nested
    @DisplayName("optional()")
    class OptionalFactory {

        @Test
        @DisplayName("둘 다 null - Optional.empty")
        void both_null_empty() {
            // when
            Optional<EventCause> result = EventCause.optional(null, null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("둘 다 값 있음 - Optional.of")
        void both_present() {
            // when
            Optional<EventCause> result = EventCause.optional("evt-1", "order-service");

            // then
            assertThat(result).contains(new EventCause("evt-1", "order-service"));
        }

        @Test
        @DisplayName("실패 - 부분 null (eventId만 있음)")
        void fail_partial_null() {
            // when & then
            assertThatThrownBy(() -> EventCause.optional("evt-1", null))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_EVENT_CAUSE);
        }
    }
}
