package boxoffice.orderservice.presentation.dto.response;

import boxoffice.orderservice.domain.entity.Order;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderSummaryResponse(
    UUID orderId,
    UUID supplierId,
    UUID receiverId,
    String status,
    int totalPrice,
    LocalDateTime createdAt,
    List<ProductItemResponse> products
) {

    public record ProductItemResponse(UUID productId, String productName, int unitPrice, int quantity) {}

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
            order.getId(),
            order.getSupplierId(),
            order.getReceiverId(),
            order.getStatus().toString(),
            order.getTotalPrice().getValue(),
            order.getCreatedAt(),
            order.getOrderProducts().stream()
                .map(p -> new ProductItemResponse(p.getProductId(), p.getProductName(), p.getUnitPrice(), p.getQuantity()))
                .toList()
        );
    }
}
