package com.boxoffice.companyservice.company.dto.response;

import java.util.UUID;

public record InternalCompanyHubResponseDto(
        UUID supplierHubId,
        UUID receiverHubId
) {
}
