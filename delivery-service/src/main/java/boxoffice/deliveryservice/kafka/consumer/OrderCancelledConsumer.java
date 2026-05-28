package boxoffice.deliveryservice.kafka.consumer;

import boxoffice.deliveryservice.domain.delivery.service.DeliveryService;
import boxoffice.deliveryservice.kafka.event.OrderCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledConsumer {

    private final DeliveryService deliveryService;

    @KafkaListener(topics = "${kafka.topic.order-cancelled}", groupId = "delivery-service")
    public void consume(OrderCancelledEvent event) {
        log.info("주문 취소 이벤트 수신 orderId={}", event.orderId());
        try {
            deliveryService.cancelDelivery(event.orderId());
        } catch (Exception e) {
            log.error("배송 취소 처리 실패 - 수동 처리 필요. orderId={}", event.orderId(), e);
        }
    }
}