package boxoffice.orderservice.application.client;

import boxoffice.orderservice.application.client.dto.request.DeliveryCancelRequest;
import boxoffice.orderservice.application.client.dto.request.DeliveryCreateRequest;
import boxoffice.orderservice.application.client.dto.response.DeliveryResponseDto;
import boxoffice.orderservice.application.client.fallback.DeliveryFeignClientFallbackFactory;
import com.boxoffice.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "delivery-service", contextId = "deliveryFeignClient", fallbackFactory = DeliveryFeignClientFallbackFactory.class)
public interface DeliveryFeignClient {
  @PostMapping("/internal/deliveries")
  ApiResponse<DeliveryResponseDto> requestDelivery(@RequestBody DeliveryCreateRequest request);

  @PostMapping("/internal/deliveries/cancel")
  void cancelDelivery(@RequestBody DeliveryCancelRequest request);
}