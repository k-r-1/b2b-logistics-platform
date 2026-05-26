package boxoffice.orderservice.application.service;

import boxoffice.orderservice.application.client.CompanyProductFeignClient;
import boxoffice.orderservice.application.client.UserFeignClient;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.application.client.dto.request.StockDeductRequest;
import boxoffice.orderservice.application.client.dto.request.StockRestoreRequest;
import boxoffice.orderservice.application.client.dto.response.InternalCompanyHub;
import boxoffice.orderservice.application.client.dto.response.StockDeductResponse;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.infra.event.OrderCreatedEvent;
import boxoffice.orderservice.infra.event.OrderEventListener;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import boxoffice.orderservice.presentation.dto.request.CreateOrderRequestDto;
import boxoffice.orderservice.presentation.dto.response.CreateOrderResponseDto;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateService {
  private final UserFeignClient userFeignClient;
  private final CompanyProductFeignClient companyProductFeignClient;
  private final OrderCommandService orderCommandService;
  private final OrderEventListener eventPublisher;

  public CreateOrderResponseDto createOrder(CreateOrderRequestDto request, String requesterId) {
    // 1. 유저 검증 및 소속 검증
    UserDetailInfo user = userFeignClient.getUserById(requesterId);
    validatePermission(user, request.supplierId(), request.receiverId());

    // 2. 상품 재고 일괄 차감
    List<StockDeductRequest> stockRequest = buildStockDeductRequest(request.products());
    StockDeductResponse response =  companyProductFeignClient.deductStocks(stockRequest);

    Order order;
    try {
      // Order
      order = orderCommandService.saveOrder(
          request.supplierId(), request.receiverId(),
          response.sourceHubId(), response.destinationHubId(),
          request.request(), response);

    } catch (Exception e) {
      log.error("주문 저장중 에러가 발생해 보상 트랜잭션이 수행됩니다.", e);
      fallback(stockRequest, requesterId);
      throw new BaseException(OrderErrorCode.ORDER_SAVE_FAILED);
    }
    eventPublisher.orderCreateEvent(buildEvent(order, request));

    return CreateOrderResponseDto.toResponse(order);
  }

  private void validatePermission(UserDetailInfo user, UUID supplierId, UUID receiverId) {
    switch (user.role()) {
      case "MASTER", "SUPPLIER_MANAGER" -> {

      }
      case "HUB_MANAGER", "DELIVERY_MANAGER" -> {
        InternalCompanyHub hubResults = companyProductFeignClient.getCompanyById(supplierId, receiverId);
        if (!user.hubId().equals(hubResults.receiverHubId()) || !user.hubId().equals(receiverId)) {
          throw new BaseException(OrderErrorCode.UNAUTHORIZED_HUB_ORDER);
        }
      }
      default -> throw new BaseException(OrderErrorCode.UNAUTHORIZED_ORDER);
    }
  }

  private List<StockDeductRequest> buildStockDeductRequest(
      List<CreateOrderRequestDto.CreateProductRequest> products
  ) {
    return products.stream()
        .map(p -> new StockDeductRequest(p.productId(), p.quantity()))
        .toList();
  }

  private OrderCreatedEvent buildEvent(Order order, CreateOrderRequestDto request) {
    return new OrderCreatedEvent(
        order.getId(),
        order.getSupplierId(),
        order.getReceiverId(),
        order.getRequest(),
        request.products().stream()
            .map(p -> new OrderCreatedEvent.ProductItem(
                p.productId(), p.quantity()
            ))
            .toList(),
        LocalDateTime.now()
    );
  }

  private void fallback(List<StockDeductRequest> stocks, String userId) {
    try {
      companyProductFeignClient.restoreStocks(
          stocks.stream()
              .map(p -> new StockRestoreRequest(p.productId(), p.quantity()))
              .toList()
          );
    } catch (Exception e) {
      log.error("[DOMAIN ERROR] 재고 복구 실패 userId={}, items={}", userId, stocks);
    }
  }
}
