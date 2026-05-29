package com.boxoffice.ainotificationservice.ai.client;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineContext;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;

// LLM 추상화. 실제 어댑터로 교체 예정이며, 메시지 생성·경로 정렬 메서드는 추후 추가.
public interface LlmClient {

    DispatchDeadlinePrediction predictDispatchDeadline(DispatchDeadlineContext context);
}
