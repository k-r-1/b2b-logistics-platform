package com.boxoffice.hubservice.stocktransfer.dto.response;

import java.util.List;
import java.util.UUID;

public record TransferPlanResponseDto(
        UUID fromHubId,
        String fromHubName,
        Integer totalStock,
        List<SuggestedTransferResponseDto> suggestedTransfers
) { }
