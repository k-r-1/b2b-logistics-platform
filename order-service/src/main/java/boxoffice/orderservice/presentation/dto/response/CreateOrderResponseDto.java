package boxoffice.orderservice.presentation.dto.response;

import boxoffice.orderservice.application.service.dto.OrderResultDto;
import boxoffice.orderservice.domain.entity.Order;
import java.util.List;
import java.util.UUID;

public record CreateOrderResponseDto(
    UUID orderId,
    UUID supplierId,
    UUID receiverId,
    UUID sourceHubId,
    UUID destinationHubId,
    String status,
    int totalPrice,
    String request,
    List<ProductItemResponse> products
) {
    public static CreateOrderResponseDto from(OrderResultDto dto) {
        return new CreateOrderResponseDto(
            dto.orderId(),
            dto.supplierId(),
            dto.receiverId(),
            dto.sourceHubId(),
            dto.destinationHubId(),
            dto.status(),
            dto.totalPrice(),
            dto.request(),
            dto.products().stream()
                .map(p -> new ProductItemResponse(p.productId(), p.productName(), p.unitPrice(), p.quantity()))
                .toList()
        );
    }

    public static CreateOrderResponseDto toResponse(Order order) {
        return new CreateOrderResponseDto(
            order.getId(),
            order.getSupplierId(),
            order.getReceiverId(),
            order.getSourceHubId(),
            order.getDestinationHubId(),
            order.getStatus().toString(),
            order.getTotalPrice().getValue(),
            order.getRequest(),
            order.getOrderProducts().stream()
                .map(p -> new ProductItemResponse(p.getProductId(), p.getProductName(), p.getUnitPrice(), p.getQuantity()))
                .toList()
        );
    }

    public record ProductItemResponse(UUID productId, String productName, int unitPrice, int quantity) {}
}
