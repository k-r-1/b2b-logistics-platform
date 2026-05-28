package boxoffice.deliveryservice.client.dto.request;

import java.util.UUID;

public record DeliveryManagerAssignRequestDto(
        UUID hubId,
        DeliveryType deliveryType
) {
    public enum DeliveryType {
        HUB_TO_HUB,
        HUB_TO_COMPANY
    }
}
