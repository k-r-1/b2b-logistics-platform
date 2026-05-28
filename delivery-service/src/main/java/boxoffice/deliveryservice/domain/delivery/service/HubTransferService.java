package boxoffice.deliveryservice.domain.delivery.service;

import boxoffice.deliveryservice.client.DeliveryManagerClient;
import boxoffice.deliveryservice.client.dto.request.DeliveryManagerAssignRequestDto;
import boxoffice.deliveryservice.client.dto.request.DeliveryManagerAssignRequestDto.DeliveryType;
import boxoffice.deliveryservice.kafka.event.HubTransferDispatchedEvent;
import boxoffice.deliveryservice.kafka.event.TransferAssignFailedEvent;
import boxoffice.deliveryservice.kafka.event.TransferAssignSuccessEvent;
import boxoffice.deliveryservice.kafka.producer.HubTransferResultProducer;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubTransferService {

    private final DeliveryManagerClient deliveryManagerClient;
    private final HubTransferResultProducer producer;

    public void handleHubTransferDispatched(HubTransferDispatchedEvent event) {
        UUID transferId = event.transferId();
        try {
            var response = deliveryManagerClient
                    .assignDeliveryManager(new DeliveryManagerAssignRequestDto(event.fromHubId(), DeliveryType.HUB_TO_HUB))
                    .getData();

            producer.publishSuccess(new TransferAssignSuccessEvent(transferId, response.deliveryManagerId()));
            log.info("허브 이전 배송 담당자 배정 성공 transferId={} managerId={}", transferId, response.deliveryManagerId());
        } catch (Exception e) {
            producer.publishFailure(new TransferAssignFailedEvent(transferId, e.getMessage()));
            log.error("허브 이전 배송 담당자 배정 실패 transferId={} reason={}", transferId, e.getMessage());
        }
    }
}
