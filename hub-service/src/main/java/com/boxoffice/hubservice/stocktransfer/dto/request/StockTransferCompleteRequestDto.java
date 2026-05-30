package com.boxoffice.hubservice.stocktransfer.dto.request;

import jakarta.validation.constraints.Size;

public record StockTransferCompleteRequestDto(
        @Size(max = 500, message = "메모는 500자 이내여야 합니다.")
        String note
) { }
