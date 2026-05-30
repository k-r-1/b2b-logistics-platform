package com.boxoffice.hubservice.stocktransfer.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockTransferCreateRequestDto(
        @NotNull(message = "출발 허브 ID는 필수입니다.")
        UUID fromHubId
) { }
