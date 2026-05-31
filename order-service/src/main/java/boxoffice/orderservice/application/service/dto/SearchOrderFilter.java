package boxoffice.orderservice.application.service.dto;

import boxoffice.orderservice.domain.enums.OrderStatus;
import java.time.LocalDate;
import java.util.UUID;

public record SearchOrderFilter(
    OrderStatus status,
    LocalDate startDate,
    LocalDate endDate,
    UUID sourceHubId,
    UUID destinationHubId
) {
    public static SearchOrderFilter empty() {
        return new SearchOrderFilter(null, null, null, null, null);
    }
}
