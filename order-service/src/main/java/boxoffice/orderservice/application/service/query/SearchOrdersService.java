package boxoffice.orderservice.application.service.query;

import boxoffice.orderservice.application.client.UserInfoCacheService;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.application.service.dto.OrderSearchPageDto;
import boxoffice.orderservice.application.service.dto.SearchOrderFilter;
import boxoffice.orderservice.domain.vo.OrderSearchCondition;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import boxoffice.orderservice.presentation.dto.response.OrderSummaryResponse;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.util.PageableUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchOrdersService {

    private final UserInfoCacheService userInfoCacheService;
    private final OrderQueryService orderQueryService;

    public Page<OrderSummaryResponse> searchOrders(String keycloakId, int page, int size, SearchOrderFilter filter) {
        UserDetailInfo user = userInfoCacheService.getUserById(keycloakId);
        OrderSearchCondition condition = buildCondition(user, filter);
        Pageable pageable = PageableUtils.ofDefault(page, size);

        if (isCacheable(user.role())) {
            OrderSearchPageDto cached = orderQueryService.searchOrdersCached(condition, pageable);
            List<OrderSummaryResponse> responses = cached.content().stream()
                .map(item -> new OrderSummaryResponse(
                    item.orderId(), item.supplierId(), item.receiverId(),
                    item.status(), item.totalPrice(), item.createdAt()))
                .toList();
            return new PageImpl<>(responses, pageable, cached.totalElements());
        }

        return orderQueryService.searchOrders(condition, pageable)
            .map(OrderSummaryResponse::from);
    }

    private boolean isCacheable(String role) {
        return "MASTER".equals(role) || "HUB_MANAGER".equals(role);
    }

    private OrderSearchCondition buildCondition(UserDetailInfo user, SearchOrderFilter filter) {
        return switch (user.role()) {
            case "SUPPLIER_MANAGER" -> {
                if (user.companyId() == null) throw new BaseException(OrderErrorCode.UNAUTHORIZED_ORDER);
                yield OrderSearchCondition.forCompany(user.companyId(), filter.status(), filter.startDate(), filter.endDate());
            }
            case "HUB_MANAGER", "DELIVERY_MANAGER" -> {
                if (user.hubId() == null) throw new BaseException(OrderErrorCode.UNAUTHORIZED_ORDER);
                boolean wantSource = filter.sourceHubId() != null;
                boolean wantDest = filter.destinationHubId() != null;
                yield OrderSearchCondition.forHub(user.hubId(), wantSource, wantDest, filter.status(), filter.startDate(), filter.endDate());
            }
            case "MASTER" -> OrderSearchCondition.forMaster(
                filter.sourceHubId(),
                filter.destinationHubId(),
                filter.status(),
                filter.startDate(),
                filter.endDate()
            );
            default -> throw new BaseException(OrderErrorCode.UNAUTHORIZED_ORDER);
        };
    }
}
