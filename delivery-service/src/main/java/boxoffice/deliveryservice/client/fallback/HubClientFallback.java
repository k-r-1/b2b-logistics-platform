package boxoffice.deliveryservice.client.fallback;

import boxoffice.deliveryservice.client.HubClient;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto;
import com.boxoffice.common.response.ApiResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class HubClientFallback implements HubClient {

    @Override
    public ApiResponse<HubRouteResponseDto> calculatePath(UUID originHubId, UUID destinationHubId) {
        return ApiResponse.success(new HubRouteResponseDto(null, null, List.of(), 0, BigDecimal.ZERO));
    }
}
