package boxoffice.deliveryservice.client.dto.response;

import java.util.UUID;

public record UserInfoDto(
        UUID id,
        UserRole role,
        UUID hubId,
        UUID companyId
) {
}