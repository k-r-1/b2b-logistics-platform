package boxoffice.orderservice.application.service;

import boxoffice.orderservice.application.client.dto.response.StockDeductResponse;
import boxoffice.orderservice.application.service.dto.CreateOrderCommand;
import boxoffice.orderservice.application.service.dto.OrderResultDto;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.entity.OrderProduct;
import boxoffice.orderservice.domain.repository.OrderRepository;
import boxoffice.orderservice.infra.config.CacheConfig;
import boxoffice.orderservice.infra.event.OrderCreatedEvent;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResultDto saveOrder(
        UUID orderId,
        UUID supplierId,
        UUID receiverId,
        UUID sourceHubId,
        UUID destinationHubId,
        String requestMsg,
        StockDeductResponse stocks,
        CreateOrderCommand command
    ) {
        List<OrderProduct> products = stocks.details().stream()
            .map(detail -> OrderProduct.create(
                detail.productId(),
                detail.productName(),
                detail.unitPrice(),
                detail.quantity()
            ))
            .toList();

        Order order = Order.create(orderId, supplierId, receiverId, sourceHubId, destinationHubId, requestMsg, products);
        Order saved = orderRepository.save(order);
        OrderResultDto result = OrderResultDto.from(saved);

        // 트랜잭션 커밋 후 @TransactionalEventListener(AFTER_COMMIT) 발동
        eventPublisher.publishEvent(buildOrderCreatedEvent(result, command));

        return result;
    }

    private OrderCreatedEvent buildOrderCreatedEvent(OrderResultDto result, CreateOrderCommand command) {
        CreateOrderCommand.DeliveryAddress addr = command.deliveryAddress();
        return new OrderCreatedEvent(
            result.orderId(),
            result.supplierId(),
            result.receiverId(),
            result.sourceHubId(),
            result.destinationHubId(),
            result.request(),
            new AddressVO(command.deliveryAddress().zipCode(), command.deliveryAddress().address(), command.deliveryAddress().detailAddress()),
            command.recipientName(),
            command.products().stream()
                .map(p -> new OrderCreatedEvent.ProductItem(p.productId(), p.quantity()))
                .toList(),
            LocalDateTime.now()
        );
    }

  @CacheEvict(value = CacheConfig.ORDER_CACHE, key = "#orderId")
  @Transactional
  public Order updateOrder(UUID orderId, String request) {
    Order order = orderRepository.findByIdWithProducts(orderId)
        .orElseThrow(() -> new BaseException(OrderErrorCode.ORDER_NOT_FOUND));
    if (request != null) {
      order.updateOrderRequest(request);
    }
    return order;
  }
}
