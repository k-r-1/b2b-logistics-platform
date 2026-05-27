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
        // @BatchSize(size=100) 적용으로 IN 절 배치 로딩 (N+1 → 2 queries)
        orders.forEach(o -> o.getOrderProducts().size());
        return orders;
    }
}
