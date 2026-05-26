package boxoffice.orderservice.infra.event;

import java.util.UUID;

public record OrderCancelledEvent(UUID orderId) {}
