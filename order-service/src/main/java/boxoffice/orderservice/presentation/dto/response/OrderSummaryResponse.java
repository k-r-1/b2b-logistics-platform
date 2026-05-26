package boxoffice.orderservice.presentation.dto.response;

import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.entity.OrderProduct;
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

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
            order.getId(),
            order.getSupplierId(),
            order.getReceiverId(),
            order.getStatus().toString(),
            order.getTotalPrice().getValue(),
            order.getCreatedAt(),
            order.getOrderProducts().stream()
                .map(ProductItemResponse::from)
                .toList()
        );
    }

    public record ProductItemResponse(
        UUID productId,
        String productName,
        int unitPrice,
        int quantity
    ) {

        public static ProductItemResponse from(OrderProduct product) {
            return new ProductItemResponse(
                product.getProductId(),
                product.getProductName(),
                product.getUnitPrice(),
                product.getQuantity()
            );
        }
    }
}
