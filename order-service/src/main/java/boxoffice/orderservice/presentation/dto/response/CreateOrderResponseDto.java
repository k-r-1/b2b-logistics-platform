package boxoffice.orderservice.presentation.dto.response;

import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.entity.OrderProduct;
import java.util.List;
import java.util.UUID;

public record CreateOrderResponseDto (
    UUID orderId,
    UUID supplierId,
    UUID receiverId,
    String status,
    int totalPrice,
    String request,
    List<ProductItemResponse> products
) {

  public static CreateOrderResponseDto toResponse(Order order) {
    return new CreateOrderResponseDto(
        order.getId(),
        order.getSupplierId(),
        order.getReceiverId(),
        order.getStatus().toString(),
        order.getTotalPrice().getValue(),
        order.getRequest(),
        order.getOrderProducts().stream()
            .map(ProductItemResponse::toResponse)
            .toList()
    );
  }

  public record ProductItemResponse(
      UUID productId,
      String productName,
      int unitPrice,
      int quantity
  ) {

    public static ProductItemResponse toResponse(OrderProduct product) {
      return new ProductItemResponse(
          product.getProductId(),
          product.getProductName(),
          product.getUnitPrice(),
          product.getQuantity()
      );
    }
  }

}
