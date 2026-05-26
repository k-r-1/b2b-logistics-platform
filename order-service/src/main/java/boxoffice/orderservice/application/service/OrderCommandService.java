package boxoffice.orderservice.application.service;

import boxoffice.orderservice.application.client.dto.response.StockDeductResponse;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.entity.OrderProduct;
import boxoffice.orderservice.domain.repository.OrderRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderCommandService {
  private final OrderRepository orderRepository;

  @Transactional
  public Order saveOrder(UUID supplierId, UUID receiverId, String request, StockDeductResponse stocks) {
    List<OrderProduct> products = stocks.details().stream()
        .map(detail -> OrderProduct.create(
            detail.productId(),
            detail.productName(),
            detail.unitPrice(),
            detail.quantity()
        ))
        .toList();

    Order order = Order.create(supplierId, receiverId, request, products);
    return orderRepository.save(order);
  }
}
