package boxoffice.deliveryservice.kafka.producer;

import boxoffice.deliveryservice.kafka.event.TransferAssignFailedEvent;
import boxoffice.deliveryservice.kafka.event.TransferAssignSuccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HubTransferResultProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.transfer-assign-success}")
    private String successTopic;

    @Value("${kafka.topic.transfer-assign-failed}")
    private String failedTopic;

    public void publishSuccess(TransferAssignSuccessEvent event) {
        kafkaTemplate.send(successTopic, event.transferId().toString(), event);
    }

    public void publishFailure(TransferAssignFailedEvent event) {
        kafkaTemplate.send(failedTopic, event.transferId().toString(), event);
    }
}