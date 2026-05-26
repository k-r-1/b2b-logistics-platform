package boxoffice.orderservice.infra.event;

import boxoffice.orderservice.application.client.CompanyProductFeignClient;
import boxoffice.orderservice.application.client.DeliveryFeignClient;
import boxoffice.orderservice.application.client.dto.request.DeliveryCancelRequest;
import boxoffice.orderservice.application.client.dto.request.DeliveryCreateRequest;
import boxoffice.orderservice.application.client.dto.request.StockRestoreRequest;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.repository.OrderRepository;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {
  private final DeliveryFeignClient deliveryFeignClient;
  private final CompanyProductFeignClient companyProductFeignClient;
  private final OrderRepository orderRepository;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void orderCreateEvent(OrderCreatedEvent event) {
    try {
      deliveryFeignClient.requestDelivery(
          new DeliveryCreateRequest(
              event.orderId(),
              event.supplierId(),
              event.receiverId(),
              event.request()
          )
      );
    } catch (Exception e) {
      log.error("[EVENT FAILED] 배송 요청이 실패했습니다. orderId = {}", event.orderId());
      cancelOrderAndRestoreStock(event);
      throw new BaseException(OrderErrorCode.DELIVERY_REQUEST_FAILED);
    }
  }

  public void orderCancelledEvent(OrderCancelledEvent event) {
    try {
      deliveryFeignClient.cancelDelivery(new DeliveryCancelRequest(event.orderId()));
    } catch (Exception e) {
      log.error("[EVENT FAILED] 배송 취소 요청이 실패했습니다. 수동 처리 필요. orderId = {}", event.orderId(), e);
    }
  }

  private void cancelOrderAndRestoreStock(OrderCreatedEvent event) {
    // 주문 취소
    try {
      Order order = orderRepository.findById(event.orderId())
          .orElseThrow(() -> new BaseException(OrderErrorCode.ORDER_NOT_FOUND));
      order.cancel();
      orderRepository.save(order);
    } catch (Exception e) {
      log.error("[EVENT_FAILED] 주문 취소가 실패되었습니다. 관리자는 해당 주문을 수동으로 변경해주세요. orderId = {}", event.orderId(), e);
    }

    // 재고 복구
    try {
      companyProductFeignClient.restoreStocks(
              event.products().stream()
                  .map(p -> new StockRestoreRequest(p.productId(), p.quantity()))
                  .toList()
      );
    } catch (Exception e) {
      log.error("DELIVERY-EVENT 재고 복구 실패. 수동처리가 필요합니다. orderId = {}", event.orderId(), e);
    }
  }
}
