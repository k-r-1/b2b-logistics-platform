package com.boxoffice.ainotificationservice.ai.cache;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// 인메모리 캐시. 실제 Redis 어댑터 도입 전 placeholder(테스트·개발용). TTL은 적용하지 않음.
public class FakeDispatchDeadlineCache implements DispatchDeadlineCache {

    private final Map<String, DispatchDeadlinePrediction> store = new ConcurrentHashMap<>();

    @Override
    public Optional<DispatchDeadlinePrediction> find(String inputHash) {
        return Optional.ofNullable(store.get(inputHash));
    }

    @Override
    public void put(String inputHash, DispatchDeadlinePrediction prediction) {
        store.put(inputHash, prediction);
    }
}
