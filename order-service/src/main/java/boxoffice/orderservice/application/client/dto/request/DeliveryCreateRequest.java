package boxoffice.orderservice.application.client.dto.request;

import java.util.UUID;

public record DeliveryCreateRequest(
    UUID orderId,
    UUID supplierId,
    UUID receiverId,
    String request
) {
}
