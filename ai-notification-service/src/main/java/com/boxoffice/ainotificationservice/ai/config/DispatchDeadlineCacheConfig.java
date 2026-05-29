package com.boxoffice.ainotificationservice.ai.config;

import com.boxoffice.ainotificationservice.ai.cache.DispatchDeadlineCache;
import com.boxoffice.ainotificationservice.ai.cache.FakeDispatchDeadlineCache;
import com.boxoffice.ainotificationservice.ai.cache.RedisDispatchDeadlineCache;
import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// 캐시 빈 선택: redis host 설정 시 Redis, 없으면 FakeDispatchDeadlineCache로 폴백.
@Configuration
public class DispatchDeadlineCacheConfig {

    @Bean
    @ConditionalOnExpression("'${spring.data.redis.host:}' != ''")
    public DispatchDeadlineCache redisDispatchDeadlineCache(RedisConnectionFactory connectionFactory) {
        JsonMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        Jackson2JsonRedisSerializer<DispatchDeadlinePrediction> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, DispatchDeadlinePrediction.class);

        RedisTemplate<String, DispatchDeadlinePrediction> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return new RedisDispatchDeadlineCache(template);
    }

    @Bean
    @ConditionalOnMissingBean(DispatchDeadlineCache.class)
    public DispatchDeadlineCache fakeDispatchDeadlineCache() {
        return new FakeDispatchDeadlineCache();
    }
}
