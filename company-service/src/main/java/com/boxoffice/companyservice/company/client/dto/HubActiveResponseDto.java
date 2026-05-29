package com.boxoffice.companyservice.company.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class HubActiveResponseDto {
    private UUID hubId;
    
    @JsonProperty("isActive")
    private boolean active;
}
