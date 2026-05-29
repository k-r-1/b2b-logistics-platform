package com.boxoffice.ainotificationservice.ai.client;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineContext;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

// Spring AI 기반 Gemini 실제 어댑터. 빈 구성은 LlmClientConfig가 api-key 설정 시 등록.
@Slf4j
public class GeminiLlmClient implements LlmClient {

    private static final String SYSTEM_PROMPT = """
            너는 물류 발송 시한 예측 전문가다. 주어진 주문·경로·배송담당자 근무시간 정보로
            납기를 맞추기 위한 '최종 발송 시한'을 계산한다.
            발송 시한은 반드시 배송담당자 근무시간 안이어야 한다.
            모든 설명(reasoning)은 한국어로 작성한다.
            """;

    private final ChatClient chatClient;

    public GeminiLlmClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public DispatchDeadlinePrediction predictDispatchDeadline(DispatchDeadlineContext context) {
        try {
            GeminiDeadlineResponse response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt(context))
                    .call()
                    .entity(GeminiDeadlineResponse.class);
            return DispatchDeadlinePrediction.llm(
                    response.dispatchDeadline(), response.reasoning(), response.confidence());
        } catch (RuntimeException e) {
            log.warn("Gemini 발송 시한 예측 호출 실패 - fallback 전환", e);
            throw new BaseException(AiErrorCode.LLM_CALL_FAILED);
        }
    }

    private String userPrompt(DispatchDeadlineContext context) {
        return """
                아래 정보로 최종 발송 시한을 계산하라.
                - 납기: %s
                - 요청사항: %s
                - 상품(이름:수량): %s
                - 발송지: %s
                - 경유지: %s
                - 도착지: %s
                - 총 예상 이동시간(초): %d
                - 배송담당자 근무시간: %s ~ %s
                """.formatted(
                context.requestedDeadline(),
                context.requesterNoteOptional().orElse("없음"),
                context.products(),
                context.route().origin(),
                context.route().waypoints(),
                context.route().destination(),
                context.totalEstimatedDuration().toSeconds(),
                context.agentWorkingHours().start(),
                context.agentWorkingHours().end());
    }
}
