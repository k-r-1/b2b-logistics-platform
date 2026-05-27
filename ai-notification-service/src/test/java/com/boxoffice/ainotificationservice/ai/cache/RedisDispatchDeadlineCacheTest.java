package com.boxoffice.ainotificationservice.ai.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@DisplayName("RedisDispatchDeadlineCache")
@ExtendWith(MockitoExtension.class)
class RedisDispatchDeadlineCacheTest {

    private static final String HASH = "a".repeat(64);
    private static final String KEY = "ai:dispatch:" + HASH;
    private static final DispatchDeadlinePrediction PREDICTION =
            DispatchDeadlinePrediction.llm(LocalDateTime.of(2026, 5, 21, 15, 30), "근거", 0.8);

    @Mock
    private RedisTemplate<String, DispatchDeadlinePrediction> redisTemplate;

    @Mock
    private ValueOperations<String, DispatchDeadlinePrediction> valueOperations;

    private RedisDispatchDeadlineCache cache;

    @BeforeEach
    void setUp() {
        cache = new RedisDispatchDeadlineCache(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Nested
    @DisplayName("find()")
    class Find {

        @Test
        @DisplayName("HIT - 키에 값이 있으면 반환")
        void hit() {
            // given
            given(valueOperations.get(KEY)).willReturn(PREDICTION);

            // when & then
            assertThat(cache.find(HASH)).contains(PREDICTION);
        }

        @Test
        @DisplayName("MISS - 키에 값이 없으면 empty")
        void miss() {
            // given
            given(valueOperations.get(KEY)).willReturn(null);

            // when & then
            assertThat(cache.find(HASH)).isEmpty();
        }
    }

    @Nested
    @DisplayName("put()")
    class Put {

        @Test
        @DisplayName("ai:dispatch 키에 TTL 24h로 저장")
        void stores_with_ttl() {
            // when
            cache.put(HASH, PREDICTION);

            // then
            verify(valueOperations).set(KEY, PREDICTION, Duration.ofHours(24));
        }
    }
}
