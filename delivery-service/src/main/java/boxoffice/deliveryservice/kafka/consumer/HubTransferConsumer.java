package boxoffice.deliveryservice.kafka.consumer;

import boxoffice.deliveryservice.domain.delivery.service.HubTransferService;
import boxoffice.deliveryservice.kafka.event.HubTransferDispatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubTransferConsumer {

    private final HubTransferService hubTransferService;

    @KafkaListener(topics = "${kafka.topic.hub-transfer-dispatched}", groupId = "delivery-service")
    public void consume(HubTransferDispatchedEvent event) {
        log.info("허브 이전 이벤트 수신 transferId={}", event.transferId());
        hubTransferService.handleHubTransferDispatched(event);
    }
}