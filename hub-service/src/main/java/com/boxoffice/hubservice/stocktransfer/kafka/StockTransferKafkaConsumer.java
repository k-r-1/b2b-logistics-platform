package com.boxoffice.hubservice.stocktransfer.kafka;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.stocktransfer.entity.StockTransfer;
import com.boxoffice.hubservice.stocktransfer.event.AssignmentFailedEvent;
import com.boxoffice.hubservice.stocktransfer.event.AssignmentSucceededEvent;
import com.boxoffice.hubservice.stocktransfer.repository.StockTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StockTransferKafkaConsumer {

    private final StockTransferRepository stockTransferRepository;

    @KafkaListener(topics = "transfer-assign-success", groupId = "hub-service")
    @Transactional
    public void onAssigned(AssignmentSucceededEvent event) {
        StockTransfer transfer = stockTransferRepository.findById(event.transferId())
                .orElseThrow(() -> new BaseException(HubErrorCode.TRANSFER_NOT_FOUND));
        transfer.assignDeliveryManager(event.deliveryManagerId());
    }

    @KafkaListener(topics = "transfer-assign-failed", groupId = "hub-service")
    @Transactional
    public void onFailed(AssignmentFailedEvent event) {
        StockTransfer transfer = stockTransferRepository.findById(event.transferId())
                .orElseThrow(() -> new BaseException(HubErrorCode.TRANSFER_NOT_FOUND));
        transfer.revertDispatch();
    }
}
