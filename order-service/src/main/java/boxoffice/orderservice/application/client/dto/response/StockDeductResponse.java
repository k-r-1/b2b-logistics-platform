package boxoffice.orderservice.application.client.dto.response;

import java.util.List;
import java.util.UUID;

public record StockDeductResponse(
    UUID sourceHubId,
    UUID destinationHubId,
    List<ProductStockResult> details
) {
  public record ProductStockResult(
      UUID productId,
      String productName,
      int unitPrice,
      int quantity
  ){}
}
