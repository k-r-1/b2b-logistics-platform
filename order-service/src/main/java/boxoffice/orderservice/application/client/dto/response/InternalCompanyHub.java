package boxoffice.orderservice.application.client.dto.response;

import java.util.UUID;

public record InternalCompanyHub(
    UUID supplierHubId,
    UUID receiverHubId
) {

}
