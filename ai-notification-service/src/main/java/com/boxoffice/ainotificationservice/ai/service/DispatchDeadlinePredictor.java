package com.boxoffice.ainotificationservice.ai.service;

import com.boxoffice.ainotificationservice.ai.cache.DispatchDeadlineCache;
import com.boxoffice.ainotificationservice.ai.client.LlmClient;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineContext;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import com.boxoffice.ainotificationservice.ai.entity.prediction.PredictionLog;
import com.boxoffice.ainotificationservice.ai.repository.PredictionLogRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 발송 시한 예측 오케스트레이션: 캐시 조회 → LLM 호출(실패 시 규칙 fallback) → 캐시·로그 적재.
@Service
@RequiredArgsConstructor
public class DispatchDeadlinePredictor {

    private final DispatchDeadlineContextHasher hasher;
    private final DispatchDeadlineCache cache;
    private final LlmClient llmClient;
    private final RuleBasedDispatchDeadlineCalculator fallbackCalculator;
    private final PredictionLogRepository predictionLogRepository;

    public DispatchDeadlinePrediction predict(DispatchDeadlineContext context) {
        String inputHash = hasher.hash(context);
        Optional<DispatchDeadlinePrediction> cached = cache.find(inputHash);
        if (cached.isPresent()) {
            return cached.get();
        }
        DispatchDeadlinePrediction prediction = predictOrFallback(context);
        // fallback은 LLM 장애 시 임시 대체값이라 캐싱하지 않는다 — 복구 후 같은 입력이 즉시 정상 예측되도록.
        if (!prediction.fallbackUsed()) {
            cache.put(inputHash, prediction);
        }
        predictionLogRepository.save(PredictionLog.of(inputHash, prediction));
        return prediction;
    }

    // LLM 호출 실패 시 규칙 기반 fallback으로 대체해 알림 유실을 방지.
    private DispatchDeadlinePrediction predictOrFallback(DispatchDeadlineContext context) {
        try {
            return llmClient.predictDispatchDeadline(context);
        } catch (RuntimeException e) {
            LocalDateTime deadline = fallbackCalculator.calculate(context.toFallbackInput());
            return DispatchDeadlinePrediction.fallback(deadline);
        }
    }
}
