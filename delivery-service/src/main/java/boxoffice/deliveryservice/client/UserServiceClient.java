package boxoffice.deliveryservice.client;

import boxoffice.deliveryservice.client.dto.response.UserInfoDto;
import com.boxoffice.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/v1/users/internal/keycloak/{keycloakSub}")
    ApiResponse<UserInfoDto> getUserBySub(@PathVariable String keycloakSub);
}