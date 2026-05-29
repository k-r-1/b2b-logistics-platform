package boxoffice.orderservice.application.client.dto.request;

import java.util.List;
import java.util.UUID;

public record StockDeductRequest(
    UUID supplierId,
    UUID receiverId,
    List<StockProducts> products
) {
  public record StockProducts(
      UUID productId,
      Integer quantity
  ) {}
}
