package boxoffice.deliveryservice.client;

import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto;
import boxoffice.deliveryservice.client.fallback.HubClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "hub-service", fallback = HubClientFallback.class)
public interface HubClient {

    @GetMapping("/internal/v1/hub-routes/path")
    HubRouteResponseDto calculatePath(
            @RequestParam UUID originHubId,
            @RequestParam UUID destinationHubId
    );
}
