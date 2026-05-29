package boxoffice.orderservice.application.client;

import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.infra.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserInfoCacheService {

    private final UserFeignClient userFeignClient;

    @Cacheable(value = CacheConfig.USER_INFO_CACHE, key = "#keycloakId", cacheManager = "caffeineCacheManager")
    public UserDetailInfo getUserById(String keycloakId) {
        return userFeignClient.getUserById(keycloakId).getData();
    }
}
