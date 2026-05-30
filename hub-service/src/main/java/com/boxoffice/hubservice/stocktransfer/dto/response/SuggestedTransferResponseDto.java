package com.boxoffice.hubservice.stocktransfer.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SuggestedTransferResponseDto(
        UUID toHubId,
        String toHubName,
        BigDecimal distanceKm,
        Integer availableCapacity,
        Integer suggestedCount,
        List<AssignedCompanyResponseDto> companies
) { }
