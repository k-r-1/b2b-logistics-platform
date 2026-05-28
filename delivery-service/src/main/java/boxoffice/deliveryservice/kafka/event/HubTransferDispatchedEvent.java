package boxoffice.deliveryservice.kafka.event;

import java.util.UUID;

public record HubTransferDispatchedEvent(
        UUID transferId,
        UUID fromHubId,
        UUID toHubId
) {
}