package com.boxoffice.ainotificationservice.ai.deadline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DeliveryRoute VO")
class DeliveryRouteTest {

    @Nested
    @DisplayName("생성자 검증")
    class Construction {

        @Test
        @DisplayName("성공 - 경유지 포함")
        void success_with_waypoints() {
            // when
            DeliveryRoute route = new DeliveryRoute("서울허브", List.of("대전허브"), "부산 해운대구");

            // then
            assertThat(route.origin()).isEqualTo("서울허브");
            assertThat(route.waypoints()).containsExactly("대전허브");
            assertThat(route.destination()).isEqualTo("부산 해운대구");
        }

        @Test
        @DisplayName("성공 - 경유지 없음(직송)")
        void success_no_waypoints() {
            // when
            DeliveryRoute route = new DeliveryRoute("서울허브", List.of(), "부산 해운대구");

            // then
            assertThat(route.waypoints()).isEmpty();
        }

        @Test
        @DisplayName("실패 - origin blank")
        void fail_blank_origin() {
            // when & then
            assertThatThrownBy(() -> new DeliveryRoute(" ", List.of(), "부산"))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }

        @Test
        @DisplayName("실패 - destination blank")
        void fail_blank_destination() {
            // when & then
            assertThatThrownBy(() -> new DeliveryRoute("서울허브", List.of(), " "))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }

        @Test
        @DisplayName("실패 - waypoints null")
        void fail_null_waypoints() {
            // when & then
            assertThatThrownBy(() -> new DeliveryRoute("서울허브", null, "부산"))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }
    }
}
