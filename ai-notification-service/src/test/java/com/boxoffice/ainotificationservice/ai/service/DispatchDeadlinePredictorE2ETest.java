package com.boxoffice.ainotificationservice.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.boxoffice.ainotificationservice.ai.deadline.DeliveryRoute;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineContext;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import com.boxoffice.ainotificationservice.ai.deadline.OrderLine;
import com.boxoffice.ainotificationservice.ai.deadline.WorkingHours;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 실제 Gemini 호출 E2E. GEMINI_API_KEY 환경변수 + postgres 필요. e2e 태그라 기본 test에서 제외.
@Tag("e2e")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
@DisplayName("DispatchDeadlinePredictor E2E (실제 Gemini)")
class DispatchDeadlinePredictorE2ETest {

    @Autowired
    private DispatchDeadlinePredictor predictor;

    @Test
    @DisplayName("실제 Gemini 호출로 근무시간 내 발송 시한을 예측한다")
    void real_gemini_prediction() {
        // given
        DispatchDeadlineContext context = new DispatchDeadlineContext(
                LocalDateTime.of(2026, 6, 1, 18, 0),
                "냉장 보관 필수",
                List.of(new OrderLine("고등어", 10)),
                new DeliveryRoute("서울 중부센터", List.of("대전허브"), "부산 해운대구 센텀로 99"),
                Duration.ofMinutes(180),
                WorkingHours.defaultHours());

        // when
        DispatchDeadlinePrediction prediction = predictor.predict(context);
        // Gemini가 준 값: dispatchDeadline / reasoning / confidence (fallbackUsed는 로컬 플래그)
        System.out.println("Gemini 예측 결과: " + prediction);

        // then
        assertThat(prediction.dispatchDeadline()).isNotNull();
        assertThat(prediction.fallbackUsed()).isFalse();
    }
}
