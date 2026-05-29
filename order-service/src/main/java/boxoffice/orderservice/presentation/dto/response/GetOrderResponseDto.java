package boxoffice.orderservice.presentation.dto.response;

import boxoffice.orderservice.application.service.dto.OrderResultDto;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.List;
import java.util.UUID;

@JsonTypeInfo(use = Id.CLASS, property = "@class")
public record GetOrderResponseDto(
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
    public static GetOrderResponseDto from(OrderResultDto dto) {
        return new GetOrderResponseDto(
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

    public record ProductItemResponse(UUID productId, String productName, int unitPrice, int quantity) {}
}
