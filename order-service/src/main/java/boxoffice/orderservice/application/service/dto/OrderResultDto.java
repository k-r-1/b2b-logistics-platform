package boxoffice.orderservice.application.service.dto;

import boxoffice.orderservice.domain.entity.Order;
import java.util.List;
import java.util.UUID;

public record OrderResultDto(
    UUID orderId,
    UUID supplierId,
    UUID receiverId,
    UUID sourceHubId,
    UUID destinationHubId,
    String status,
    int totalPrice,
    String request,
    List<ProductItemDto> products
) {
    public record ProductItemDto(UUID productId, String productName, int unitPrice, int quantity) {}

    public static OrderResultDto from(Order order) {
        return new OrderResultDto(
            order.getId(),
            order.getSupplierId(),
            order.getReceiverId(),
            order.getSourceHubId(),
            order.getDestinationHubId(),
            order.getStatus().name(),
            order.getTotalPrice().getValue(),
            order.getRequest(),
            order.getOrderProducts().stream()
                .map(p -> new ProductItemDto(
                    p.getProductId(), p.getProductName(), p.getUnitPrice(), p.getQuantity()
                ))
                .toList()
        );
    }
}
