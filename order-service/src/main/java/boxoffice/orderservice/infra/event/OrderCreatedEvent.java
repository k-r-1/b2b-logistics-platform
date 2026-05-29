package boxoffice.orderservice.infra.event;

import com.boxoffice.common.entity.AddressVO;
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
    AddressVO deliveryAddress,
    String recipientName,
    List<ProductItem> products,
    LocalDateTime publishedAt
) {
  public record ProductItem(UUID productId, int quantity) {}
}
