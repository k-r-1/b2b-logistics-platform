package com.boxoffice.ainotificationservice.ai.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FakeDispatchDeadlineCache")
class FakeDispatchDeadlineCacheTest {

    private static final String HASH = "a".repeat(64);

    private final FakeDispatchDeadlineCache cache = new FakeDispatchDeadlineCache();

    @Nested
    @DisplayName("find()")
    class Find {

        @Test
        @DisplayName("put 후 같은 키로 조회하면 HIT")
        void hit_after_put() {
            // given
            DispatchDeadlinePrediction prediction =
                    DispatchDeadlinePrediction.llm(LocalDateTime.of(2026, 5, 21, 15, 30), "근거", 0.8);
            cache.put(HASH, prediction);

            // when
            var found = cache.find(HASH);

            // then
            assertThat(found).contains(prediction);
        }

        @Test
        @DisplayName("저장 안 된 키는 MISS")
        void miss_when_absent() {
            // when & then
            assertThat(cache.find(HASH)).isEmpty();
        }

        @Test
        @DisplayName("같은 키 재저장 시 최신 값으로 덮어씀")
        void overwrite() {
            // given
            cache.put(HASH, DispatchDeadlinePrediction.fallback(LocalDateTime.of(2026, 5, 21, 14, 0)));
            DispatchDeadlinePrediction latest =
                    DispatchDeadlinePrediction.llm(LocalDateTime.of(2026, 5, 21, 15, 30), "근거", 0.8);

            // when
            cache.put(HASH, latest);

            // then
            assertThat(cache.find(HASH)).contains(latest);
        }
    }
}
