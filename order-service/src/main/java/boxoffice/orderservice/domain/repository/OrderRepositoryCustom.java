package boxoffice.orderservice.domain.repository;

import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.vo.OrderSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {

    Page<Order> searchOrders(OrderSearchCondition condition, Pageable pageable);
}
