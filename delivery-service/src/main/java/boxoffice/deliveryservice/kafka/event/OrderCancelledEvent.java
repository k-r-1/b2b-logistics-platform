package boxoffice.deliveryservice.kafka.event;

import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId
) { }