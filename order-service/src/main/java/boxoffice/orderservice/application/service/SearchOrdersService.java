package boxoffice.orderservice.application.service;

import boxoffice.orderservice.application.client.UserFeignClient;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.vo.OrderSearchCondition;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import boxoffice.orderservice.presentation.dto.response.OrderSummaryResponse;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchOrdersService {

    private final UserFeignClient userFeignClient;
    private final OrderQueryService orderQueryService;

    public Page<OrderSummaryResponse> searchOrders(String keycloakId, int page, int size) {
        UserDetailInfo user = userFeignClient.getUserById(keycloakId);
        OrderSearchCondition condition = buildCondition(user);
        Pageable pageable = PageableUtils.ofDefault(page, size);
        return orderQueryService.searchOrders(condition, pageable)
            .map(OrderSummaryResponse::from);
    }

    private OrderSearchCondition buildCondition(UserDetailInfo user) {
        return switch (user.role()) {
            case "SUPPLIER_MANAGER" -> OrderSearchCondition.forCompany(user.companyId());
            case "HUB_MANAGER", "DELIVERY_MANAGER" -> OrderSearchCondition.forHub(user.hubId());
            case "MASTER" -> OrderSearchCondition.forMaster();
            default -> throw new BaseException(OrderErrorCode.UNAUTHORIZED_ORDER);
        };
    }
}
