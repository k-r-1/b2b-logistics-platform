package com.boxoffice.ainotificationservice.notification.entity.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Recipient VO")
class RecipientTest {

    @Nested
    @DisplayName("팩터리")
    class Factories {

        @Test
        @DisplayName("user() - USER 타입 Recipient 생성")
        void user_factory() {
            // when
            Recipient recipient = Recipient.user("U1");

            // then
            assertThat(recipient).isEqualTo(new Recipient("U1", RecipientType.USER));
        }

        @Test
        @DisplayName("channel() - CHANNEL 타입 Recipient 생성")
        void channel_factory() {
            // when
            Recipient recipient = Recipient.channel("C1");

            // then
            assertThat(recipient).isEqualTo(new Recipient("C1", RecipientType.CHANNEL));
        }
    }

    @Nested
    @DisplayName("검증")
    class Validation {

        @Test
        @DisplayName("실패 - null id")
        void fail_null_id() {
            // when & then
            assertThatThrownBy(() -> new Recipient(null, RecipientType.USER))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_RECIPIENT);
        }

        @Test
        @DisplayName("실패 - 빈 id")
        void fail_blank_id() {
            // when & then
            assertThatThrownBy(() -> new Recipient("  ", RecipientType.USER))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_RECIPIENT);
        }

        @Test
        @DisplayName("실패 - null type")
        void fail_null_type() {
            // when & then
            assertThatThrownBy(() -> new Recipient("U1", null))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_RECIPIENT);
        }
    }
}
