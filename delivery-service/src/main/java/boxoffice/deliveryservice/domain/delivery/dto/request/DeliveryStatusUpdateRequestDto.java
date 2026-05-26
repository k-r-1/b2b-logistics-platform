package boxoffice.deliveryservice.domain.delivery.dto.request;

import boxoffice.deliveryservice.domain.delivery.entity.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record DeliveryStatusUpdateRequestDto(
        @NotNull DeliveryStatus status
) {
}