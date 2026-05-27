package com.boxoffice.ainotificationservice.ai.deadline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DispatchDeadlineContext VO")
class DispatchDeadlineContextTest {

    private static final LocalDateTime REQUESTED_DEADLINE = LocalDateTime.of(2026, 5, 21, 18, 0);
    private static final Duration ESTIMATED_DURATION = Duration.ofMinutes(120);

    private final WorkingHours hours = WorkingHours.defaultHours();
    private final List<OrderLine> products = List.of(new OrderLine("고등어", 10));
    private final DeliveryRoute route = new DeliveryRoute("서울허브", List.of("대전허브"), "부산 해운대구");

    private DispatchDeadlineContext context(String requesterNote) {
        return new DispatchDeadlineContext(
                REQUESTED_DEADLINE, requesterNote, products, route, ESTIMATED_DURATION, hours);
    }

    @Nested
    @DisplayName("생성자 검증")
    class Construction {

        @Test
        @DisplayName("성공 - 유효한 입력")
        void success() {
            // when
            DispatchDeadlineContext ctx = context("냉장 보관 필수");

            // then
            assertThat(ctx.requestedDeadline()).isEqualTo(REQUESTED_DEADLINE);
            assertThat(ctx.products()).containsExactly(new OrderLine("고등어", 10));
            assertThat(ctx.route()).isEqualTo(route);
        }

        @Test
        @DisplayName("실패 - requestedDeadline null")
        void fail_null_deadline() {
            // when & then
            assertThatThrownBy(() -> new DispatchDeadlineContext(
                    null, "note", products, route, ESTIMATED_DURATION, hours))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }

        @Test
        @DisplayName("실패 - totalEstimatedDuration 음수")
        void fail_negative_duration() {
            // given
            Duration negative = Duration.ofMinutes(-1);

            // when & then
            assertThatThrownBy(() -> new DispatchDeadlineContext(
                    REQUESTED_DEADLINE, "note", products, route, negative, hours))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }

        @Test
        @DisplayName("실패 - route null")
        void fail_null_route() {
            // when & then
            assertThatThrownBy(() -> new DispatchDeadlineContext(
                    REQUESTED_DEADLINE, "note", products, null, ESTIMATED_DURATION, hours))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }

        @Test
        @DisplayName("실패 - products 빈 리스트")
        void fail_empty_products() {
            // when & then
            assertThatThrownBy(() -> new DispatchDeadlineContext(
                    REQUESTED_DEADLINE, "note", List.of(), route, ESTIMATED_DURATION, hours))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }

        @Test
        @DisplayName("실패 - agentWorkingHours null")
        void fail_null_working_hours() {
            // when & then
            assertThatThrownBy(() -> new DispatchDeadlineContext(
                    REQUESTED_DEADLINE, "note", products, route, ESTIMATED_DURATION, null))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_DISPATCH_INPUT);
        }
    }

    @Nested
    @DisplayName("toFallbackInput()")
    class ToFallbackInput {

        @Test
        @DisplayName("납기·이동시간·근무시간만 추출")
        void extracts_fallback_fields() {
            // given
            DispatchDeadlineContext ctx = context("note");

            // when
            DispatchDeadlineInput fallback = ctx.toFallbackInput();

            // then
            assertThat(fallback.requestedDeadline()).isEqualTo(REQUESTED_DEADLINE);
            assertThat(fallback.totalEstimatedDuration()).isEqualTo(ESTIMATED_DURATION);
            assertThat(fallback.agentWorkingHours()).isEqualTo(hours);
        }
    }

    @Nested
    @DisplayName("requesterNoteOptional()")
    class RequesterNote {

        @Test
        @DisplayName("값 있으면 present")
        void present() {
            // when & then
            assertThat(context("냉장 보관 필수").requesterNoteOptional()).contains("냉장 보관 필수");
        }

        @Test
        @DisplayName("null이면 empty")
        void empty() {
            // when & then
            assertThat(context(null).requesterNoteOptional()).isEmpty();
        }
    }
}
