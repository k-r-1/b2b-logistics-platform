package boxoffice.orderservice.infra.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
    UUID orderId,
    UUID supplierId,
    UUID receiverId,
    String request,
    List<ProductItem> products,
    LocalDateTime publishedAt
) {
  public record ProductItem(
      UUID productId,
      int quantity
  ) {}
}
