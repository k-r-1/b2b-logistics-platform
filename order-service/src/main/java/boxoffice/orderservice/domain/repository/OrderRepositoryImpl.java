package boxoffice.orderservice.domain.repository;

import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.entity.QOrder;
import boxoffice.orderservice.domain.vo.OrderSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
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

        long total;
        if (pageable.getPageNumber() == 0) {
            total = Optional.ofNullable(queryFactory
                .select(QOrder.order.count())
                .from(QOrder.order)
                .where(predicate)
                .fetchOne()
            ).orElse(0L);
        } else {
            // 첫 페이지 이후는 COUNT 생략 — hasNext 패턴으로 전체 건수 추정
            long offset = pageable.getOffset();
            total = content.size() == pageable.getPageSize()
                ? offset + content.size() + 1
                : offset + content.size();
        }

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanBuilder buildPredicate(OrderSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        // 접근 제어: SUPPLIER_MANAGER (OR 조건)
        if (condition.companyId() != null) {
            builder.and(
                QOrder.order.supplierId.eq(condition.companyId())
                    .or(QOrder.order.receiverId.eq(condition.companyId()))
            );
        }

        // 접근 제어: HUB_MANAGER 방향 필터 없을 때 (OR 조건)
        if (condition.hubId() != null) {
            builder.and(
                QOrder.order.sourceHubId.eq(condition.hubId())
                    .or(QOrder.order.destinationHubId.eq(condition.hubId()))
            );
        }

        // 허브 방향 필터 (HUB_MANAGER 방향 지정 or MASTER 특정 허브 지정)
        if (condition.filterSourceHubId() != null) {
            builder.and(QOrder.order.sourceHubId.eq(condition.filterSourceHubId()));
        }
        if (condition.filterDestinationHubId() != null) {
            builder.and(QOrder.order.destinationHubId.eq(condition.filterDestinationHubId()));
        }

        // 주문 상태 필터
        if (condition.status() != null) {
            builder.and(QOrder.order.status.eq(condition.status()));
        }

        // 날짜 범위 필터 (createdAt 기준)
        if (condition.startDate() != null) {
            builder.and(QOrder.order.createdAt.goe(condition.startDate().atStartOfDay()));
        }
        if (condition.endDate() != null) {
            builder.and(QOrder.order.createdAt.loe(condition.endDate().atTime(LocalTime.MAX)));
        }

        return builder;
    }

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
