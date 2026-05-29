package com.boxoffice.ainotificationservice.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.boxoffice.ainotificationservice.ai.cache.FakeDispatchDeadlineCache;
import com.boxoffice.ainotificationservice.ai.client.FakeLlmClient;
import com.boxoffice.ainotificationservice.ai.deadline.DeliveryRoute;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineContext;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import com.boxoffice.ainotificationservice.ai.deadline.OrderLine;
import com.boxoffice.ainotificationservice.ai.deadline.WorkingHours;
import com.boxoffice.ainotificationservice.ai.repository.PredictionLogRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("DispatchDeadlinePredictor")
@ExtendWith(MockitoExtension.class)
class DispatchDeadlinePredictorTest {

    private static final LocalDateTime EXPECTED_DEADLINE = LocalDateTime.of(2026, 5, 21, 15, 30);
    private static final DispatchDeadlineContext CONTEXT = new DispatchDeadlineContext(
            LocalDateTime.of(2026, 5, 21, 18, 0),
            "냉장",
            List.of(new OrderLine("고등어", 10)),
            new DeliveryRoute("서울허브", List.of("대전허브"), "부산 해운대구"),
            Duration.ofMinutes(120),
            WorkingHours.defaultHours());

    private final DispatchDeadlineContextHasher hasher = new DispatchDeadlineContextHasher();
    private final RuleBasedDispatchDeadlineCalculator fallbackCalculator = new RuleBasedDispatchDeadlineCalculator();

    @Mock
    private PredictionLogRepository predictionLogRepository;

    private DispatchDeadlinePredictor predictor(FakeDispatchDeadlineCache cache, FakeLlmClient llm) {
        return new DispatchDeadlinePredictor(hasher, cache, llm, fallbackCalculator, predictionLogRepository);
    }

    @Nested
    @DisplayName("predict()")
    class Predict {

        @Test
        @DisplayName("캐시 MISS - LLM 예측 후 캐시·로그 적재")
        void miss_calls_llm() {
            // given
            FakeDispatchDeadlineCache cache = new FakeDispatchDeadlineCache();
            FakeLlmClient llm = new FakeLlmClient();

            // when
            DispatchDeadlinePrediction prediction = predictor(cache, llm).predict(CONTEXT);

            // then
            assertThat(prediction.dispatchDeadline()).isEqualTo(EXPECTED_DEADLINE);
            assertThat(prediction.fallbackUsed()).isFalse();
            assertThat(llm.recordedInputs()).containsExactly(CONTEXT);
            assertThat(cache.find(hasher.hash(CONTEXT))).contains(prediction);
            verify(predictionLogRepository).save(any());
        }

        @Test
        @DisplayName("캐시 HIT - LLM 미호출, 캐시값 반환, 로그 미적재")
        void hit_skips_llm() {
            // given
            FakeDispatchDeadlineCache cache = new FakeDispatchDeadlineCache();
            DispatchDeadlinePrediction cachedPrediction =
                    DispatchDeadlinePrediction.llm(LocalDateTime.of(2026, 5, 21, 12, 0), "캐시", 0.9);
            cache.put(hasher.hash(CONTEXT), cachedPrediction);
            FakeLlmClient llm = new FakeLlmClient();

            // when
            DispatchDeadlinePrediction prediction = predictor(cache, llm).predict(CONTEXT);

            // then
            assertThat(prediction).isEqualTo(cachedPrediction);
            assertThat(llm.recordedInputs()).isEmpty();
            verifyNoInteractions(predictionLogRepository);
        }

        @Test
        @DisplayName("LLM 실패 - 규칙 fallback으로 대체")
        void llm_failure_falls_back() {
            // given
            FakeDispatchDeadlineCache cache = new FakeDispatchDeadlineCache();
            FakeLlmClient llm = new FakeLlmClient(context -> {
                throw new IllegalStateException("LLM down");
            });

            // when
            DispatchDeadlinePrediction prediction = predictor(cache, llm).predict(CONTEXT);

            // then
            assertThat(prediction.fallbackUsed()).isTrue();
            assertThat(prediction.dispatchDeadline()).isEqualTo(EXPECTED_DEADLINE);
            assertThat(cache.find(hasher.hash(CONTEXT))).isEmpty();
            verify(predictionLogRepository).save(any());
        }
    }
}
