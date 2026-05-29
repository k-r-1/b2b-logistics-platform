package com.boxoffice.ainotificationservice.ai.config;

import com.boxoffice.ainotificationservice.ai.client.FakeLlmClient;
import com.boxoffice.ainotificationservice.ai.client.GeminiLlmClient;
import com.boxoffice.ainotificationservice.ai.client.LlmClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// LLM 어댑터 빈 선택: api-key 설정 시 실제 Gemini, 없으면 FakeLlmClient로 폴백.
@Configuration
public class LlmClientConfig {

    @Bean
    @ConditionalOnExpression("'${spring.ai.google.genai.api-key:}' != ''")
    public LlmClient geminiLlmClient(ChatModel chatModel) {
        return new GeminiLlmClient(ChatClient.create(chatModel));
    }

    @Bean
    @ConditionalOnMissingBean(LlmClient.class)
    public LlmClient fakeLlmClient() {
        return new FakeLlmClient();
    }
}
