package com.boxoffice.ainotificationservice.notification.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultTemplateRenderer")
class DefaultTemplateRendererTest {

    private final TemplateRenderer renderer =
            new DefaultTemplateRenderer(new InMemoryTemplateRepository());

    @Nested
    @DisplayName("render()")
    class Render {

        @Test
        @DisplayName("성공 - USER_APPROVED 단일 키 치환")
        void success_user_approved() {
            String result = renderer.render(new UserApprovedContext("홍길동"));

            assertThat(result).isEqualTo("안녕하세요 홍길동님, 가입이 승인되었습니다.");
        }

        @Test
        @DisplayName("성공 - ORDER_CANCELED 다중 키 치환")
        void success_order_canceled_multiple_keys() {
            String result = renderer.render(new OrderCanceledContext("ORD-42", "재고 부족"));

            assertThat(result).contains("ORD-42").contains("재고 부족");
        }

        @Test
        @DisplayName("실패 - repository에 본문이 없으면 TEMPLATE_NOT_FOUND")
        void fail_template_not_found() {
            TemplateRepository empty = type -> Optional.empty();
            TemplateRenderer noRepo = new DefaultTemplateRenderer(empty);

            assertThatThrownBy(() -> noRepo.render(new UserApprovedContext("test")))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.TEMPLATE_NOT_FOUND);
        }
    }
}
