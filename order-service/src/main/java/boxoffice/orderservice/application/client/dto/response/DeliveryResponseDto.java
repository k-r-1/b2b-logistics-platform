package boxoffice.orderservice.application.client.dto.response;

import com.boxoffice.common.entity.AddressVO;
import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryResponseDto(
    UUID id,
    UUID orderId,
    UUID originHubId,
    UUID destinationHubId,
    AddressVO deliveryAddress,
    UUID deliveryPersonId,
    String recipientName,
    String recipientSlackId,
    String deliveryStatus,
    LocalDateTime createdAt
) {

}
