package boxoffice.orderservice.presentation.dto.response;

import boxoffice.orderservice.domain.entity.Order;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSummaryResponse(
    UUID orderId,
    UUID supplierId,
    UUID receiverId,
    String status,
    int totalPrice,
    LocalDateTime createdAt
) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
            order.getId(),
            order.getSupplierId(),
            order.getReceiverId(),
            order.getStatus().toString(),
            order.getTotalPrice().getValue(),
            order.getCreatedAt()
        );
    }
}
