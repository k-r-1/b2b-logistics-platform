package boxoffice.orderservice.application.service.query;

import boxoffice.orderservice.application.client.UserInfoCacheService;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.application.service.dto.OrderResultDto;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetOrderService {

    private final UserInfoCacheService userInfoCacheService;
    private final OrderQueryService orderQueryService;
    private final MeterRegistry meterRegistry;

    public OrderResultDto getOrder(UUID orderId, String keycloakId) {
        MDC.put("orderId", orderId.toString());
        MDC.put("requesterId", keycloakId);
        try {
            return Timer.builder("order.getOrder.total")
                .description("GetOrderService.getOrder 전체 응답 시간")
                .register(meterRegistry)
                .record(() -> doGetOrder(orderId, keycloakId));
        } finally {
            MDC.remove("orderId");
            MDC.remove("requesterId");
        }
    }

    private OrderResultDto doGetOrder(UUID orderId, String keycloakId) {
        UserDetailInfo user = Timer.builder("order.userFeign.getUserById")
            .description("UserFeignClient.getUserById 레이턴시")
            .register(meterRegistry)
            .record(() -> userInfoCacheService.getUserById(keycloakId));

        log.info("[GetOrder] 유저 정보 조회 성공. role={}", user.role());

        OrderResultDto order = orderQueryService.findByIdAsDto(orderId);
        validateOwnership(user, order);

        log.info("[GetOrder] 주문 조회 완료");
        return order;
    }

    private void validateOwnership(UserDetailInfo user, OrderResultDto order) {
        switch (user.role()) {
            case "MASTER" -> { }
            case "COMPANY_MANAGER" -> {
                if (!order.supplierId().equals(user.companyId()) && !order.receiverId().equals(user.companyId())) {
                    throw new BaseException(OrderErrorCode.ORDER_NOT_FOUND);
                }
            }
            case "HUB_MANAGER", "DELIVERY_MANAGER" -> {
                boolean accessible = (order.sourceHubId() != null && order.sourceHubId().equals(user.hubId()))
                    || (order.destinationHubId() != null && order.destinationHubId().equals(user.hubId()));
                if (!accessible) {
                    throw new BaseException(OrderErrorCode.ORDER_NOT_FOUND);
                }
            }
            default -> throw new BaseException(OrderErrorCode.UNAUTHORIZED_ORDER);
        }
    }
}
