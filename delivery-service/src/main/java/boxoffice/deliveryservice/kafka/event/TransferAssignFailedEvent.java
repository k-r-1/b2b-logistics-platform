package boxoffice.deliveryservice.kafka.event;

import java.util.UUID;

public record TransferAssignFailedEvent(
        UUID transferId,
        String reason
) {
}