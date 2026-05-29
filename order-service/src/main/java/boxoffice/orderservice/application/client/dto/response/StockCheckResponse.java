package boxoffice.orderservice.application.client.dto.response;

import java.util.List;
import java.util.UUID;

public record StockCheckResponse(
    boolean isAllOrderable,
    List<StockDetail> details
) {
  public record StockDetail(
      UUID productId,
      String productName,
      Integer unitPrice,
      boolean isOrderable
  ) {}
}
