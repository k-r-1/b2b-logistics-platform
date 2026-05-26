package boxoffice.orderservice.application.client;

import boxoffice.orderservice.application.client.dto.request.DeliveryCancelRequest;
import boxoffice.orderservice.application.client.dto.request.DeliveryCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "delivery-service")
public interface DeliveryFeignClient {
  @PostMapping("/internal/deliveries")
  void requestDelivery(@RequestBody DeliveryCreateRequest request);

  @PostMapping("/internal/deliveries/cancel")
  void cancelDelivery(@RequestBody DeliveryCancelRequest request);
}
