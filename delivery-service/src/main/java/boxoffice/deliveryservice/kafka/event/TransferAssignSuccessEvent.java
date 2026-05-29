package boxoffice.deliveryservice.kafka.event;

import java.util.UUID;

public record TransferAssignSuccessEvent(
        UUID transferId,
        UUID deliveryManagerId
) {
}