package boxoffice.orderservice.application.service.command;

import boxoffice.orderservice.application.client.CompanyProductFeignClient;
import boxoffice.orderservice.application.client.UserFeignClient;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.application.client.dto.request.StockDeductRequest;
import boxoffice.orderservice.application.client.dto.request.StockDeductRequest.StockProducts;
import boxoffice.orderservice.application.client.dto.request.StockRestoreRequest;
import boxoffice.orderservice.application.client.dto.response.InternalCompanyHub;
import boxoffice.orderservice.application.client.dto.response.StockDeductResponse;
import boxoffice.orderservice.application.service.dto.CreateOrderCommand;
import boxoffice.orderservice.application.service.dto.OrderResultDto;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateService {

    private final UserFeignClient userFeignClient;
    private final CompanyProductFeignClient companyProductFeignClient;
    private final OrderCommandService orderCommandService;

    public OrderResultDto createOrder(CreateOrderCommand command, String requesterId) {
        UUID orderId = UUID.randomUUID();
        MDC.put("orderId", orderId.toString());
        MDC.put("requesterId", requesterId);
        try {
            return doCreateOrder(orderId, command, requesterId);
        } finally {
            MDC.remove("orderId");
            MDC.remove("requesterId");
        }
    }

    private OrderResultDto doCreateOrder(UUID orderId, CreateOrderCommand command, String requesterId) {
        // 1. 유저 정보 조회
        UserDetailInfo user = userFeignClient.getUserById(requesterId).getData();
        log.info("[CreateOrder] 유저 정보 조회 성공. role={}", user.role());

        // 2. 권한 검증 및 receiverId 보정
        UUID effectiveReceiverId = resolveReceiverId(user, command.supplierId(), command.receiverId());

        // 3. 재고 차감 - 사전 생성된 orderId 전달 (이력 추적 및 멱등성 보장)
        StockDeductRequest stockRequests = buildStockDeductRequests(effectiveReceiverId, command);
        StockDeductResponse stockResponse = companyProductFeignClient.deductStocks(orderId, stockRequests).getData();
        log.info("[CreateOrder] 재고 차감 성공");

        // 4. 주문 저장 (트랜잭션 내 이벤트 발행 포함)
        try {
            OrderResultDto result = orderCommandService.saveOrder(
                orderId,
                command.supplierId(),
                effectiveReceiverId,
                stockResponse.sourceHubId(),
                stockResponse.destinationHubId(),
                command.request(),
                stockResponse,
                command
            );
            log.info("[CreateOrder] 주문 생성 완료");
            return result;

        } catch (Exception e) {
            log.error("[CreateOrder] 주문 저장 실패, 재고 보상 트랜잭션 수행", e);
            restoreStockOrLog(orderId, stockRequests);
            throw new BaseException(OrderErrorCode.ORDER_SAVE_FAILED);
        }
    }

    private UUID resolveReceiverId(UserDetailInfo user, UUID supplierId, UUID receiverId) {
        return switch (user.role()) {
            case "MASTER" -> receiverId;
            case "COMPANY_MANAGER" -> {
                // COMPANY_MANAGER는 본인 업체(companyId)가 수신자로 강제 대체됨
                log.info("[CreateOrder] COMPANY_MANAGER 권한: receiverId를 companyId로 대체");
                yield user.companyId();
            }
            case "HUB_MANAGER", "DELIVERY_MANAGER" -> {
                InternalCompanyHub hubs = companyProductFeignClient.getCompanyById(supplierId, receiverId).getData();
                boolean isRelatedHub = user.hubId().equals(hubs.supplierHubId())
                    || user.hubId().equals(hubs.receiverHubId());
                if (!isRelatedHub) {
                    throw new BaseException(OrderErrorCode.UNAUTHORIZED_HUB_ORDER);
                }
                yield receiverId;
            }
            default -> throw new BaseException(OrderErrorCode.UNAUTHORIZED_ORDER);
        };
    }

    private StockDeductRequest buildStockDeductRequests(UUID receiverId, CreateOrderCommand command) {
        return new StockDeductRequest(
            command.supplierId(),
            receiverId,
            command.products().stream()
                .map(p -> new StockProducts(p.productId(), p.quantity()))
                .toList()
        );
    }

    private void restoreStockOrLog(UUID orderId, StockDeductRequest stockRequests) {
        try {
            companyProductFeignClient.restoreStocks(
                orderId,
                stockRequests.products().stream()
                    .map(p -> new StockRestoreRequest(p.productId(), p.quantity()))
                    .toList()
            );
        } catch (Exception e) {
            log.error("[CreateOrder] 재고 복구 실패 - 수동 처리 필요. orderId={}", orderId, e);
        }
    }
}
