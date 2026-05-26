package boxoffice.orderservice.application.service;

import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.repository.OrderRepository;
import boxoffice.orderservice.domain.vo.OrderSearchCondition;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public Page<Order> searchOrders(OrderSearchCondition condition, Pageable pageable) {
        Page<Order> orders = orderRepository.searchOrders(condition, pageable);
        // @ElementCollection 지연 로딩 강제 초기화 (N+1 발생 - docs 참조)
        orders.forEach(o -> o.getOrderProducts().size());
        return orders;
    }
}
