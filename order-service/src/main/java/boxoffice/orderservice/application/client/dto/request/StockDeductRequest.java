package boxoffice.orderservice.application.client.dto.request;

import java.util.UUID;

public record StockDeductRequest(
    UUID productId,
    Integer quantity
) {
}
