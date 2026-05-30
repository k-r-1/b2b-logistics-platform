package com.boxoffice.hubservice.stocktransfer.kafka;

import com.boxoffice.hubservice.stocktransfer.event.TransferDispatchedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StockTransferKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendDispatched(TransferDispatchedEvent event) {
        kafkaTemplate.send("hub-transfer-dispatched", event);
    }
}
