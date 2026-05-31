package boxoffice.orderservice.application.service.dto;

import boxoffice.orderservice.domain.entity.Order;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public record OrderSearchPageDto(
    List<OrderSummaryItem> content,
    long totalElements,
    int pageNumber,
    int pageSize
) {

    public record OrderSummaryItem(
        UUID orderId,
        UUID supplierId,
        UUID receiverId,
        String status,
        int totalPrice,
        LocalDateTime createdAt
    ) {
        public static OrderSummaryItem from(Order order) {
            return new OrderSummaryItem(
                order.getId(),
                order.getSupplierId(),
                order.getReceiverId(),
                order.getStatus().name(),
                order.getTotalPrice().getValue(),
                order.getCreatedAt()
            );
        }
    }

    public static OrderSearchPageDto from(Page<Order> page) {
        return new OrderSearchPageDto(
            page.getContent().stream().map(OrderSummaryItem::from).toList(),
            page.getTotalElements(),
            page.getNumber(),
            page.getSize()
        );
    }
}
