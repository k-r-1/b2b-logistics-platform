package com.boxoffice.hubservice.client;

import java.util.List;
import java.util.UUID;

public record BulkHubTransferRequestDto(
        List<UUID> companyIds,
        UUID toHubId
) { }
