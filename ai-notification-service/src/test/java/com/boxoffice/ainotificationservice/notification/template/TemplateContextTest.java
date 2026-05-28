package com.boxoffice.ainotificationservice.notification.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TemplateContext records")
class TemplateContextTest {

    @Nested
    @DisplayName("UserApprovedContext")
    class UserApproved {

        @Test
        @DisplayName("성공 - {name} 치환")
        void render_replaces_name() {
            // given
            UserApprovedContext ctx = new UserApprovedContext("홍길동");

            // when & then
            assertThat(ctx.render("안녕하세요 {name}님")).isEqualTo("안녕하세요 홍길동님");
        }

        @Test
        @DisplayName("실패 - 빈 name")
        void fail_blank_name() {
            // when & then
            assertThatThrownBy(() -> new UserApprovedContext(" "))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.TEMPLATE_CONTEXT_MISSING);
        }
    }

    @Nested
    @DisplayName("UserRejectedContext")
    class UserRejected {

        @Test
        @DisplayName("성공 - {name}, {reason} 치환")
        void render_replaces_keys() {
            // given
            UserRejectedContext ctx = new UserRejectedContext("홍길동", "서류 미비");

            // when & then
            assertThat(ctx.render("{name}: {reason}")).isEqualTo("홍길동: 서류 미비");
        }

        @Test
        @DisplayName("실패 - 빈 reason")
        void fail_blank_reason() {
            // when & then
            assertThatThrownBy(() -> new UserRejectedContext("홍길동", " "))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("MasterSignupRequestContext")
    class MasterSignupRequest {

        @Test
        @DisplayName("성공 - {name}, {email}, {role} 치환")
        void render_replaces_keys() {
            // given
            MasterSignupRequestContext ctx = new MasterSignupRequestContext("홍길동", "x@y.z", "MASTER");

            // when & then
            assertThat(ctx.render("{name}/{email}/{role}")).isEqualTo("홍길동/x@y.z/MASTER");
        }

        @Test
        @DisplayName("실패 - null role")
        void fail_null_role() {
            // when & then
            assertThatThrownBy(() -> new MasterSignupRequestContext("홍길동", "x@y.z", null))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("OrderCanceledContext")
    class OrderCanceled {

        @Test
        @DisplayName("성공 - {orderId}, {reason} 치환")
        void render_replaces_keys() {
            // given
            OrderCanceledContext ctx = new OrderCanceledContext("ORD-1", "재고 부족");

            // when & then
            assertThat(ctx.render("#{orderId} - {reason}")).isEqualTo("#ORD-1 - 재고 부족");
        }

        @Test
        @DisplayName("실패 - null reason")
        void fail_null_reason() {
            // when & then
            assertThatThrownBy(() -> new OrderCanceledContext("ORD-1", null))
                    .isInstanceOf(BaseException.class);
        }
    }
}
