package boxoffice.deliveryservice.domain.delivery.dto.request;

import com.boxoffice.common.entity.AddressVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeliveryCreateRequestDto(
        @NotNull UUID orderId,
        @NotNull UUID companyId,
        @NotNull UUID originHubId,
        @NotNull UUID destinationHubId,
        @Valid @NotNull AddressVO deliveryAddress,
        @NotBlank String recipientName,
        String recipientSlackId
) { }
