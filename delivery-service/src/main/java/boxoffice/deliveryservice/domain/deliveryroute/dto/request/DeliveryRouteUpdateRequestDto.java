package boxoffice.deliveryservice.domain.deliveryroute.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DeliveryRouteUpdateRequestDto(
        @NotNull @DecimalMin("0.0") BigDecimal actualDistance,
        @NotNull @Min(0) Integer actualDuration
) {
}