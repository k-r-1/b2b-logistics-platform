package com.boxoffice.ainotificationservice.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.boxoffice.ainotificationservice.ai.deadline.DeliveryRoute;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineContext;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import com.boxoffice.ainotificationservice.ai.deadline.OrderLine;
import com.boxoffice.ainotificationservice.ai.deadline.WorkingHours;
import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@DisplayName("GeminiLlmClient")
@ExtendWith(MockitoExtension.class)
class GeminiLlmClientTest {

    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 5, 21, 15, 30);
    private static final DispatchDeadlineContext CONTEXT = new DispatchDeadlineContext(
            LocalDateTime.of(2026, 5, 21, 18, 0),
            "냉장",
            List.of(new OrderLine("고등어", 10)),
            new DeliveryRoute("서울허브", List.of("대전허브"), "부산 해운대구"),
            Duration.ofMinutes(120),
            WorkingHours.defaultHours());

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    private GeminiLlmClient client;

    @BeforeEach
    void setUp() {
        client = new GeminiLlmClient(chatClient);
    }

    private void givenChatChain() {
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.system(anyString())).willReturn(requestSpec);
        given(requestSpec.user(anyString())).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callSpec);
    }

    @Nested
    @DisplayName("predictDispatchDeadline()")
    class Predict {

        @Test
        @DisplayName("성공 - 구조화 응답을 LLM 예측으로 변환")
        void success() {
            // given
            givenChatChain();
            given(callSpec.entity(GeminiDeadlineResponse.class))
                    .willReturn(new GeminiDeadlineResponse(DEADLINE, "이동시간 고려", 0.9));

            // when
            DispatchDeadlinePrediction prediction = client.predictDispatchDeadline(CONTEXT);

            // then
            assertThat(prediction.dispatchDeadline()).isEqualTo(DEADLINE);
            assertThat(prediction.fallbackUsed()).isFalse();
            assertThat(prediction.reasoningOptional()).contains("이동시간 고려");
            assertThat(prediction.confidenceOptional()).contains(0.9);
        }

        @Test
        @DisplayName("실패 - 호출 예외는 LLM_CALL_FAILED로 변환")
        void wraps_failure() {
            // given
            givenChatChain();
            given(callSpec.entity(GeminiDeadlineResponse.class))
                    .willThrow(new RuntimeException("API error"));

            // when & then
            assertThatThrownBy(() -> client.predictDispatchDeadline(CONTEXT))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.LLM_CALL_FAILED);
        }
    }
}
