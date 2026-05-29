package boxoffice.orderservice.application.client.dto.request;

import java.util.UUID;

public record StockCheckRequest (
    UUID productId,
    Integer quantity
){
}
