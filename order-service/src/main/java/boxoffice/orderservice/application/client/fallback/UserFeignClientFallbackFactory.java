package boxoffice.orderservice.application.client.fallback;

import boxoffice.orderservice.application.client.UserFeignClient;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserFeignClientFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        return userId -> {
            log.error("[UserFeignClient] 유저 서비스 호출 실패. userId={}", userId, cause);
            throw new BaseException(OrderErrorCode.USER_SERVICE_UNAVAILABLE);
        };
    }
}
