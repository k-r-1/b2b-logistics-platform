package com.boxoffice.ainotificationservice.ai.client;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineContext;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// 실제 LLM 어댑터 도입 전 placeholder. 기본 전략은 결정론적 테스트용 임의값.
public class FakeLlmClient implements LlmClient {

    private static final Duration DEFAULT_MARGIN = Duration.ofMinutes(30);
    private static final String DEFAULT_REASONING = "fake-llm: requestedDeadline - totalEstimatedDuration - 30m";
    private static final Double DEFAULT_CONFIDENCE = 0.8;

    private final List<DispatchDeadlineContext> inputHistory = new ArrayList<>();
    private final Function<DispatchDeadlineContext, DispatchDeadlinePrediction> strategy;

    public FakeLlmClient() {
        this(FakeLlmClient::defaultStrategy);
    }

    public FakeLlmClient(Function<DispatchDeadlineContext, DispatchDeadlinePrediction> strategy) {
        if (strategy == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
        this.strategy = strategy;
    }

    @Override
    public DispatchDeadlinePrediction predictDispatchDeadline(DispatchDeadlineContext context) {
        inputHistory.add(context);
        return strategy.apply(context);
    }

    public List<DispatchDeadlineContext> recordedInputs() {
        return List.copyOf(inputHistory);
    }

    private static DispatchDeadlinePrediction defaultStrategy(DispatchDeadlineContext context) {
        LocalDateTime deadline = context.requestedDeadline()
                .minus(context.totalEstimatedDuration())
                .minus(DEFAULT_MARGIN);
        return DispatchDeadlinePrediction.llm(deadline, DEFAULT_REASONING, DEFAULT_CONFIDENCE);
    }
}
