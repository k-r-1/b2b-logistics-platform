package boxoffice.orderservice.application.client.dto.request;

import com.boxoffice.common.entity.AddressVO;
import java.util.UUID;

public record DeliveryCreateRequest(
    UUID orderId,
    UUID originHubId,
    UUID destinationHubId,
    AddressVO deliveryAddress,
    String recipientName,
    String recipientSlackId
) {
}
