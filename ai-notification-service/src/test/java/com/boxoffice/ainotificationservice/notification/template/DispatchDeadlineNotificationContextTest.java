package com.boxoffice.ainotificationservice.notification.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.common.exception.BaseException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DispatchDeadlineNotificationContext")
class DispatchDeadlineNotificationContextTest {

    @Test
    @DisplayName("성공 - 담당자명·주문·발송시한·사유 치환")
    void render_replaces_keys() {
        DispatchDeadlineNotificationContext ctx = new DispatchDeadlineNotificationContext(
                "김배송", "ORD-1", LocalDateTime.of(2026, 6, 1, 14, 30), "납기 역산 결과");

        String result = ctx.render(
                "{agentName}님, 주문 #{orderId}의 발송 시한은 {deadline} 입니다. (사유: {reasoning})");

        assertThat(result)
                .isEqualTo("김배송님, 주문 #ORD-1의 발송 시한은 2026-06-01 14:30 입니다. (사유: 납기 역산 결과)");
    }

    @Test
    @DisplayName("실패 - dispatchDeadline null")
    void fail_null_deadline() {
        assertThatThrownBy(() -> new DispatchDeadlineNotificationContext("김배송", "ORD-1", null, "x"))
                .isInstanceOf(BaseException.class);
    }
}
