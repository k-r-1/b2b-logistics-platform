package boxoffice.orderservice.domain.repository;

import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.entity.QOrder;
import boxoffice.orderservice.domain.vo.OrderSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> searchOrders(OrderSearchCondition condition, Pageable pageable) {
        BooleanBuilder predicate = buildPredicate(condition);

        List<Order> content = queryFactory
            .selectFrom(QOrder.order)
            .where(predicate)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(QOrder.order.createdAt.desc())
            .fetch();

        long total = queryFactory
            .select(QOrder.order.count())
            .from(QOrder.order)
            .where(predicate)
            .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanBuilder buildPredicate(OrderSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.companyId() != null) {
            builder.and(
                QOrder.order.supplierId.eq(condition.companyId())
                    .or(QOrder.order.receiverId.eq(condition.companyId()))
            );
        }

        if (condition.hubId() != null) {
            builder.and(
                QOrder.order.sourceHubId.eq(condition.hubId())
                    .or(QOrder.order.destinationHubId.eq(condition.hubId()))
            );
        }

        return builder;
    }
}
