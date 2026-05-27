package com.boxoffice.companyservice.company.client;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.company.client.dto.HubActiveResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "hub-service", path = "/internal/hubs")
public interface HubClient {

    @GetMapping("/{hubId}/active")
    ApiResponse<HubActiveResponseDto> checkHubActive(@PathVariable("hubId") UUID hubId);
}
