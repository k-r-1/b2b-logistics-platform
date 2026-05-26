package boxoffice.orderservice.application.service;

import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.repository.OrderRepository;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Order findById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BaseException(OrderErrorCode.ORDER_NOT_FOUND));
        order.getOrderProducts().size();
        return order;
    }
}
