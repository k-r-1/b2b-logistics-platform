package boxoffice.deliveryservice.client;

import boxoffice.deliveryservice.client.dto.request.DeliveryManagerAssignRequestDto;
import boxoffice.deliveryservice.client.dto.response.DeliveryManagerAssignResponseDto;
import com.boxoffice.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "delivery-manager-service")
public interface DeliveryManagerClient {

    @PostMapping("/internal/v1/delivery-managers/assign")
    ApiResponse<DeliveryManagerAssignResponseDto> assignDeliveryManager(
            @RequestBody DeliveryManagerAssignRequestDto request
    );
}
