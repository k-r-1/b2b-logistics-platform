package com.boxoffice.ainotificationservice.ai.deadline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OrderLine VO")
class OrderLineTest {

    @Nested
    @DisplayName("생성자 검증")
    class Construction {

        @Test
        @DisplayName("성공 - 유효한 상품·수량")
        void success() {
            // when
            OrderLine line = new OrderLine("고등어", 10);

            // then
            assertThat(line.productName()).isEqualTo("고등어");
            assertThat(line.quantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("실패 - productName blank")
        void fail_blank_name() {
            // when & then
            assertThatThrownBy(() -> new OrderLine(" ", 10))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }

        @Test
        @DisplayName("실패 - quantity 0 이하")
        void fail_non_positive_quantity() {
            // when & then
            assertThatThrownBy(() -> new OrderLine("고등어", 0))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }
    }
}
