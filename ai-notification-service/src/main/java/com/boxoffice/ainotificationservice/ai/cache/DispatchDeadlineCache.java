package com.boxoffice.ainotificationservice.ai.cache;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import java.util.Optional;

// 발송 시한 예측 캐시. 키는 입력 해시. 실제 구현은 Redis(TTL 24h), 테스트·개발은 Fake.
public interface DispatchDeadlineCache {

    Optional<DispatchDeadlinePrediction> find(String inputHash);

    void put(String inputHash, DispatchDeadlinePrediction prediction);
}
