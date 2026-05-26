package boxoffice.orderservice.application.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserFeignClient {
  @GetMapping("/internal/{id}")
  UserDetailInfo getUsersById(@PathVariable String id);
}
