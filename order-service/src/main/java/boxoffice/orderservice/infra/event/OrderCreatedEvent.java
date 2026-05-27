package boxoffice.orderservice.infra.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
    UUID orderId,
    UUID supplierId,
    UUID receiverId,
    UUID sourceHubId,
    UUID destinationHubId,
    String request,
    DeliveryAddress deliveryAddress,
    String recipientName,
    String recipientSlackId,
    List<ProductItem> products,
    LocalDateTime publishedAt
) {
  public record ProductItem(UUID productId, int quantity) {}

  public record DeliveryAddress(String zipCode, String address, String detailAddress) {}
}
