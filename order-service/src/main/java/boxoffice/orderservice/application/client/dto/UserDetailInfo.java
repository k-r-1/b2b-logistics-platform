package boxoffice.orderservice.application.client.dto;

import java.util.UUID;

public record UserDetailInfo(
    UUID userId,
    String email,
    String username,
    String role,
    UUID hubId,
    String status,
    UUID companyId
) {
}
