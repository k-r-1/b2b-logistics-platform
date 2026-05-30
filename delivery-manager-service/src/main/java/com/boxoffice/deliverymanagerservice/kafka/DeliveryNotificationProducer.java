package com.boxoffice.deliverymanagerservice.kafka;

import com.boxoffice.deliverymanagerservice.kafka.event.DeliveryAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryNotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "delivery-manager.events";

    public void sendDeliveryAssignedEvent(DeliveryAssignedEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getDeliveryId(), event);
            log.info("[Kafka Produce] DeliveryAssigned 이벤트 발행 완료. EventId: {}, DeliveryId: {}", event.getEventId(), event.getDeliveryId());
        } catch (Exception e) {
            log.error("[Kafka Produce Error] 이벤트 발행 실패. EventId: {}", event.getEventId(), e);
            // TODO: 향후 프로덕션 환경 도입 시 Spring Retry 또는 DLQ(Dead Letter Queue)를 활용한 재시도 로직 추가 필요
        }
    }
}