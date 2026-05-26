package boxoffice.orderservice.application.client.dto.request;

import java.util.UUID;

public record StockRestoreRequest(
    UUID productId,
    Integer quantity
) {
}
