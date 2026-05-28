package boxoffice.orderservice.domain.repository;

import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.entity.QOrder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Order> findByIdWithProducts(UUID orderId) {
        QOrder order = QOrder.order;

        Order result = queryFactory
            .selectFrom(order)
            .leftJoin(order.orderProducts).fetchJoin()
            .where(order.id.eq(orderId))
            .fetchOne();

        return Optional.ofNullable(result);
    }
}
