package com.boxoffice.ainotificationservice.ai.deadline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WorkingHours VO")
class WorkingHoursTest {

    @Nested
    @DisplayName("생성자 검증")
    class Construction {

        @Test
        @DisplayName("성공 - defaultHours 09:00 ~ 18:00")
        void success_default() {
            assertThat(WorkingHours.defaultHours())
                    .isEqualTo(new WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0)));
        }

        @Test
        @DisplayName("실패 - start >= end")
        void fail_start_not_before_end() {
            LocalTime start = LocalTime.of(18, 0);
            LocalTime end = LocalTime.of(9, 0);

            assertThatThrownBy(() -> new WorkingHours(start, end))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_WORKING_HOURS);
        }

        @Test
        @DisplayName("실패 - null start")
        void fail_null_start() {
            LocalTime end = LocalTime.of(18, 0);

            assertThatThrownBy(() -> new WorkingHours(null, end))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.INVALID_WORKING_HOURS);
        }
    }

    @Nested
    @DisplayName("adjustToWithin()")
    class AdjustToWithin {

        private final WorkingHours hours = WorkingHours.defaultHours();

        @Test
        @DisplayName("근무시간 안: 그대로 반환")
        void within() {
            LocalDateTime input = LocalDateTime.of(2026, 5, 21, 12, 0);

            assertThat(hours.adjustToWithin(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("근무시간 종료 후: 같은 날 종료 시각으로 당김")
        void after_end() {
            LocalDateTime input = LocalDateTime.of(2026, 5, 21, 23, 0);

            assertThat(hours.adjustToWithin(input))
                    .isEqualTo(LocalDateTime.of(2026, 5, 21, 18, 0));
        }

        @Test
        @DisplayName("근무시간 시작 전: 전날 종료 시각으로 당김")
        void before_start() {
            LocalDateTime input = LocalDateTime.of(2026, 5, 21, 7, 0);

            assertThat(hours.adjustToWithin(input))
                    .isEqualTo(LocalDateTime.of(2026, 5, 20, 18, 0));
        }

        @Test
        @DisplayName("종료 시각 정각: 종료 시각으로 당김(경계 포함)")
        void exactly_end_time() {
            LocalDateTime input = LocalDateTime.of(2026, 5, 21, 18, 0);

            assertThat(hours.adjustToWithin(input))
                    .isEqualTo(LocalDateTime.of(2026, 5, 21, 18, 0));
        }
    }
}
