package boxoffice.orderservice.application.service;

import boxoffice.orderservice.application.client.UserFeignClient;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import boxoffice.orderservice.presentation.dto.response.CreateOrderResponseDto;
import com.boxoffice.common.exception.BaseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOrderService {

    private final UserFeignClient userFeignClient;
    private final OrderQueryService orderQueryService;

    public CreateOrderResponseDto getOrder(UUID orderId, String keycloakId) {
        UserDetailInfo user = userFeignClient.getUserById(keycloakId);
        Order order = orderQueryService.findById(orderId);
        validateOwnership(user, order);
        return CreateOrderResponseDto.toResponse(order);
    }

    private void validateOwnership(UserDetailInfo user, Order order) {
        switch (user.role()) {
            case "MASTER" -> { }
            case "SUPPLIER_MANAGER" -> {
                boolean accessible = order.getSupplierId().equals(user.companyId())
                    || order.getReceiverId().equals(user.companyId());
                if (!accessible) throw new BaseException(OrderErrorCode.ORDER_NOT_FOUND);
            }
            case "HUB_MANAGER", "DELIVERY_MANAGER" -> {
                boolean accessible = (order.getSourceHubId() != null && order.getSourceHubId().equals(user.hubId()))
                    || (order.getDestinationHubId() != null && order.getDestinationHubId().equals(user.hubId()));
                if (!accessible) throw new BaseException(OrderErrorCode.ORDER_NOT_FOUND);
            }
            default -> throw new BaseException(OrderErrorCode.UNAUTHORIZED_ORDER);
        }
    }
}
