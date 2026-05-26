package boxoffice.deliveryservice.domain.delivery.dto.response;

import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.delivery.entity.DeliveryStatus;
import com.boxoffice.common.entity.AddressVO;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryResponseDto(
        UUID id,
        UUID orderId,
        UUID companyId,
        UUID originHubId,
        UUID destinationHubId,
        AddressVO deliveryAddress,
        UUID deliveryPersonId,
        String recipientName,
        String recipientSlackId,
        DeliveryStatus deliveryStatus,
        LocalDateTime createdAt
) {
    public static DeliveryResponseDto from(Delivery delivery) {
        return new DeliveryResponseDto(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getCompanyId(),
                delivery.getOriginHubId(),
                delivery.getDestinationHubId(),
                delivery.getDeliveryAddress(),
                delivery.getDeliveryPersonId(),
                delivery.getRecipientName(),
                delivery.getRecipientSlackId(),
                delivery.getDeliveryStatus(),
                delivery.getCreatedAt()
        );
    }
}
