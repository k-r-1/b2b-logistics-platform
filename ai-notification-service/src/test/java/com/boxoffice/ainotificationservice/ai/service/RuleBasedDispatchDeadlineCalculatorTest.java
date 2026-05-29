package com.boxoffice.ainotificationservice.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineInput;
import com.boxoffice.ainotificationservice.ai.deadline.WorkingHours;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RuleBasedDispatchDeadlineCalculator")
class RuleBasedDispatchDeadlineCalculatorTest {

    private final RuleBasedDispatchDeadlineCalculator calculator = new RuleBasedDispatchDeadlineCalculator();
    private final WorkingHours hours = new WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0));

    @Nested
    @DisplayName("calculate()")
    class Calculate {

        @Test
        @DisplayName("성공 - 근무시간 안: 납기 - 이동시간 - 30분 그대로")
        void within_working_hours() {
            // given
            DispatchDeadlineInput input = new DispatchDeadlineInput(
                    LocalDateTime.of(2026, 5, 21, 18, 0),
                    Duration.ofMinutes(120),
                    hours
            );

            // when
            LocalDateTime result = calculator.calculate(input);

            // then
            assertThat(result).isEqualTo(LocalDateTime.of(2026, 5, 21, 15, 30));
        }

        @Test
        @DisplayName("성공 - 근무시간 종료 후로 떨어지면 같은 날 18:00로 당김")
        void adjust_after_working_hours() {
            // given
            DispatchDeadlineInput input = new DispatchDeadlineInput(
                    LocalDateTime.of(2026, 5, 22, 23, 0),
                    Duration.ofMinutes(60),
                    hours
            );

            // when
            LocalDateTime result = calculator.calculate(input);

            // then
            assertThat(result).isEqualTo(LocalDateTime.of(2026, 5, 22, 18, 0));
        }

        @Test
        @DisplayName("성공 - 근무시간 시작 전으로 떨어지면 전날 18:00로 당김")
        void adjust_before_working_hours() {
            // given
            DispatchDeadlineInput input = new DispatchDeadlineInput(
                    LocalDateTime.of(2026, 5, 21, 10, 0),
                    Duration.ofMinutes(120),
                    hours
            );

            // when
            LocalDateTime result = calculator.calculate(input);

            // then
            assertThat(result).isEqualTo(LocalDateTime.of(2026, 5, 20, 18, 0));
        }
    }
}
