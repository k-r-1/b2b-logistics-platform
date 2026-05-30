package com.boxoffice.companyservice.product.dto.response;

import lombok.Getter;

import java.util.UUID;

@Getter
public class HubStockCountResponseDto {

    private final UUID hubId;
    private final Long stockCount;

    public HubStockCountResponseDto(UUID hubId, Long stockCount) {
        this.hubId = hubId;
        this.stockCount = stockCount;
    }
}
