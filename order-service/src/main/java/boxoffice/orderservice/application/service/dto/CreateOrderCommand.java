package boxoffice.orderservice.application.service.dto;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
    UUID supplierId,
    UUID receiverId,
    String request,
    List<ProductItem> products,
    DeliveryAddress deliveryAddress,
    String recipientName
) {
    public record ProductItem(UUID productId, Integer quantity) {}

    public record DeliveryAddress(String zipCode, String address, String detailAddress) {}
}
