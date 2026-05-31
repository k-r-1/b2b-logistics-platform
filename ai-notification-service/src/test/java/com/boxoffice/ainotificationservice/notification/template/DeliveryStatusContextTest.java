package com.boxoffice.ainotificationservice.notification.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.common.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DeliveryStatusContext")
class DeliveryStatusContextTest {

    private static final String TEMPLATE = "배송 #{deliveryId} (주문 #{orderId}) {statusText}. {detail}";

    @Test
    @DisplayName("ARRIVED_AT_DESTINATION - 수령인 표시")
    void render_arrived() {
        DeliveryStatusContext ctx = new DeliveryStatusContext(
                "DLV-1", "ORD-1", DeliveryStatus.ARRIVED_AT_DESTINATION, "홍길동", null);

        assertThat(ctx.render(TEMPLATE))
                .isEqualTo("배송 #DLV-1 (주문 #ORD-1) 도착지에 도착했습니다. 수령인: 홍길동");
    }

    @Test
    @DisplayName("FAILED - 실패 사유 표시")
    void render_failed() {
        DeliveryStatusContext ctx = new DeliveryStatusContext(
                "DLV-2", "ORD-2", DeliveryStatus.FAILED, null, "주소 불명");

        assertThat(ctx.render(TEMPLATE))
                .isEqualTo("배송 #DLV-2 (주문 #ORD-2) 배송에 실패했습니다. 사유: 주소 불명");
    }

    @Test
    @DisplayName("선택 필드 없음 - 미상으로 표기")
    void render_missing_optional() {
        DeliveryStatusContext ctx = new DeliveryStatusContext(
                "DLV-3", "ORD-3", DeliveryStatus.DELIVERED, null, null);

        assertThat(ctx.render(TEMPLATE)).contains("배송이 완료되었습니다").contains("수령인: 미상");
    }

    @Test
    @DisplayName("실패 - status null")
    void fail_null_status() {
        assertThatThrownBy(() -> new DeliveryStatusContext("DLV-1", "ORD-1", null, null, null))
                .isInstanceOf(BaseException.class);
    }
}
