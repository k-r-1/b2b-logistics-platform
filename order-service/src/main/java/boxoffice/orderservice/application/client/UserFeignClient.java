package boxoffice.orderservice.application.client;

import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.application.client.fallback.UserFeignClientFallbackFactory;
import com.boxoffice.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", contextId = "userFeignClient", fallbackFactory = UserFeignClientFallbackFactory.class, primary = false)
public interface UserFeignClient {
  @GetMapping("/internal/{id}")
  ApiResponse<UserDetailInfo> getUserById(@PathVariable String id);
}
