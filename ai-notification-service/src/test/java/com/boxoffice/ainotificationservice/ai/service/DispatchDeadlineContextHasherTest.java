package com.boxoffice.ainotificationservice.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.boxoffice.ainotificationservice.ai.deadline.DeliveryRoute;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineContext;
import com.boxoffice.ainotificationservice.ai.deadline.OrderLine;
import com.boxoffice.ainotificationservice.ai.deadline.WorkingHours;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DispatchDeadlineContextHasher")
class DispatchDeadlineContextHasherTest {

    private final DispatchDeadlineContextHasher hasher = new DispatchDeadlineContextHasher();

    private DispatchDeadlineContext context(LocalDateTime deadline, String note) {
        return new DispatchDeadlineContext(
                deadline,
                note,
                List.of(new OrderLine("고등어", 10)),
                new DeliveryRoute("서울허브", List.of("대전허브"), "부산 해운대구"),
                Duration.ofMinutes(120),
                WorkingHours.defaultHours());
    }

    @Nested
    @DisplayName("hash()")
    class Hash {

        @Test
        @DisplayName("같은 입력은 같은 해시 (결정론)")
        void deterministic() {
            // given
            LocalDateTime deadline = LocalDateTime.of(2026, 5, 21, 18, 0);

            // when
            String first = hasher.hash(context(deadline, "냉장"));
            String second = hasher.hash(context(deadline, "냉장"));

            // then
            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("SHA-256 hex 64자")
        void hex_length() {
            // when
            String hash = hasher.hash(context(LocalDateTime.of(2026, 5, 21, 18, 0), "냉장"));

            // then
            assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("납기가 다르면 해시도 다름")
        void differs_by_deadline() {
            // given
            String a = hasher.hash(context(LocalDateTime.of(2026, 5, 21, 18, 0), "냉장"));

            // when
            String b = hasher.hash(context(LocalDateTime.of(2026, 5, 22, 18, 0), "냉장"));

            // then
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("요청사항 유무로 해시가 갈림")
        void differs_by_note() {
            // given
            LocalDateTime deadline = LocalDateTime.of(2026, 5, 21, 18, 0);
            String withNote = hasher.hash(context(deadline, "냉장"));

            // when
            String withoutNote = hasher.hash(context(deadline, null));

            // then
            assertThat(withNote).isNotEqualTo(withoutNote);
        }
    }
}
