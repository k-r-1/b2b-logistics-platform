package boxoffice.deliveryservice.domain.deliveryroute.dto.request;

import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRouteStatus;
import jakarta.validation.constraints.NotNull;

public record DeliveryRouteStatusUpdateRequestDto(
        @NotNull DeliveryRouteStatus status
) {
}