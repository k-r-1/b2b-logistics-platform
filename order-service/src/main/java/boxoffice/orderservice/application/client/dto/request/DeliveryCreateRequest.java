package boxoffice.orderservice.application.client.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DeliveryCreateRequest(
    UUID orderId,
    UUID originHubId,
    UUID destinationHubId,
    AddressRequest deliveryAddress,
    String recipientName,
    String recipientSlackId
) {
  public record AddressRequest(
      @Size(max = 10) String zipCode,
      @NotBlank String address,
      @Size(max = 255) String detailAddress
  ) {}
}
