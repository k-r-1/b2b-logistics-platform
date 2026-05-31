package boxoffice.orderservice.application.service.command;

import boxoffice.orderservice.application.client.CompanyProductFeignClient;
import boxoffice.orderservice.application.client.UserFeignClient;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.application.client.dto.request.StockRestoreRequest;
import boxoffice.orderservice.application.service.query.OrderQueryService;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.enums.OrderStatus;
import boxoffice.orderservice.infra.event.OrderCancelledEvent;
import boxoffice.orderservice.infra.event.OrderEventListener;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteOrderService {

    private final UserFeignClient userFeignClient;
    private final CompanyProductFeignClient companyProductFeignClient;
    private final OrderQueryService orderQueryService;
    private final OrderCommandService orderCommandService;
    private final OrderEventListener orderEventListener;

    public void deleteOrder(UUID orderId, String keycloakId) {
        UserDetailInfo user = userFeignClient.getUserById(keycloakId).getData();
        validateRole(user);

        Order order = orderQueryService.findById(orderId);

        if (user.isHubManager()) {
            validateHubOwnership(user, order);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BaseException(OrderErrorCode.CANCEL_NOT_ALLOWED);
        }

        restoreStockOrLog(order, orderId);

        orderCommandService.cancelOrder(order, user.userId());
        orderQueryService.evictSearchCache();

        orderEventListener.orderCancelledEvent(new OrderCancelledEvent(orderId));
    }

    private void validateRole(UserDetailInfo user) {
        switch (user.role()) {
            case "MASTER", "HUB_MANAGER" -> {}
            default -> throw new BaseException(OrderErrorCode.UNAUTHORIZED_ORDER);
        }
    }

    private void validateHubOwnership(UserDetailInfo user, Order order) {
        boolean accessible =
            (order.getSourceHubId() != null && order.getSourceHubId().equals(user.hubId()))
            || (order.getDestinationHubId() != null && order.getDestinationHubId().equals(user.hubId()));
        if (!accessible) {
            throw new BaseException(OrderErrorCode.UNAUTHORIZED_HUB_ORDER);
        }
    }

    private void restoreStockOrLog(Order order, UUID orderId) {
        try {
            List<StockRestoreRequest> restoreRequests = order.getOrderProducts().stream()
                .map(p -> new StockRestoreRequest(p.getProductId(), p.getQuantity()))
                .toList();
            companyProductFeignClient.restoreStocks(orderId, restoreRequests);
        } catch (Exception e) {
            log.error("[DeleteOrder] 재고 복구 실패 - 수동 처리 필요. orderId={}", orderId, e);
        }
    }
}
