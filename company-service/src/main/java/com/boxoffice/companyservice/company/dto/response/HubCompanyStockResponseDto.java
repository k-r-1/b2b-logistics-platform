package com.boxoffice.companyservice.company.dto.response;

import lombok.Getter;

import java.util.UUID;

@Getter
public class HubCompanyStockResponseDto {

    private final UUID companyId;
    private final String companyName;
    private final Long stockCount;

    public HubCompanyStockResponseDto(UUID companyId, String companyName, Long stockCount) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.stockCount = stockCount;
    }
}
