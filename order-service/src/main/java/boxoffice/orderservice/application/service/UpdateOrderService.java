package boxoffice.orderservice.application.service;

import boxoffice.orderservice.application.client.UserFeignClient;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.enums.OrderStatus;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import boxoffice.orderservice.presentation.dto.request.UpdateOrderRequest;
import boxoffice.orderservice.presentation.dto.response.CreateOrderResponseDto;
import com.boxoffice.common.exception.BaseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateOrderService {

    private final UserFeignClient userFeignClient;
    private final OrderQueryService orderQueryService;
    private final OrderCommandService orderCommandService;

    public CreateOrderResponseDto updateOrder(UUID orderId, UpdateOrderRequest request, String keycloakId) {
        UserDetailInfo user = userFeignClient.getUserById(keycloakId).getData();
        validateRole(user);

        Order order = orderQueryService.findById(orderId);

        if (user.isHubManager()) {
            validateHubOwnership(user, order);
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BaseException(OrderErrorCode.ORDER_ALREADY_DELIVERED);
        }

        Order updated = orderCommandService.updateOrder(orderId, request.request());
        return CreateOrderResponseDto.toResponse(updated);
    }

    private void validateRole(UserDetailInfo user) {
        switch (user.role()) {
            case "MASTER", "HUB_MANAGER" -> {}
          default -> throw new BaseException(OrderErrorCode.UNAUTHORIZED_ORDER);
        }
    }

    private void validateHubOwnership(UserDetailInfo user, Order order) {
        boolean accessible = (order.getSourceHubId() != null && order.getSourceHubId().equals(user.hubId()))
            || (order.getDestinationHubId() != null && order.getDestinationHubId().equals(user.hubId()));
        if (!accessible) {
            throw new BaseException(OrderErrorCode.UNAUTHORIZED_HUB_ORDER);
        }
    }
}
