package com.boxoffice.ainotificationservice.ai.cache;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;

// Redis 기반 발송 시한 예측 캐시. 키 ai:dispatch:{hash}
public class RedisDispatchDeadlineCache implements DispatchDeadlineCache {

    private static final String KEY_PREFIX = "ai:dispatch:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, DispatchDeadlinePrediction> redisTemplate;

    public RedisDispatchDeadlineCache(RedisTemplate<String, DispatchDeadlinePrediction> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<DispatchDeadlinePrediction> find(String inputHash) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + inputHash));
    }

    @Override
    public void put(String inputHash, DispatchDeadlinePrediction prediction) {
        redisTemplate.opsForValue().set(KEY_PREFIX + inputHash, prediction, TTL);
    }
}
