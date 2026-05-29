package boxoffice.orderservice.infra.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@EnableCaching
@Configuration
public class CacheConfig {

    public static final String ORDER_CACHE = "order";
    public static final String USER_INFO_CACHE = "userInfo";

    @Value("${app.cache.order-ttl:60}")
    private long orderTtlSeconds;

    @Value("${app.cache.user-info-ttl:10}")
    private long userInfoTtlSeconds;

    @Primary
    @Bean("redisCacheManager")
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
            );

        RedisCacheConfiguration orderConfig = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(Duration.ofSeconds(orderTtlSeconds))
            .prefixCacheNameWith("v2:")
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(mapper)));

        return RedisCacheManager.builder(cf)
            .cacheDefaults(orderConfig)
            .build();
    }

    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(USER_INFO_CACHE);
        manager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(userInfoTtlSeconds, TimeUnit.SECONDS)
                .maximumSize(1000)
        );
        return manager;
    }
}
