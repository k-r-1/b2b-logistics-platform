package com.boxoffice.ainotificationservice.ai.client;

import java.time.LocalDateTime;

// Gemini 구조화 출력 매핑용. ChatClient.entity()가 JSON Schema 생성·응답 파싱에 사용.
public record GeminiDeadlineResponse(
        LocalDateTime dispatchDeadline,
        String reasoning,
        Double confidence
) {

}
