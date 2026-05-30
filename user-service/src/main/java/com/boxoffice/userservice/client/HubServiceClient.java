package com.boxoffice.userservice.client;

import com.boxoffice.userservice.dto.HubManagerRegisterRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.UUID;

@FeignClient(name = "hub-service", path = "/internal/v1/hubs")
public interface HubServiceClient {

    @PatchMapping("/{hubId}/manager")
    void registerHubManager(@PathVariable("hubId") UUID hubId,
                            @RequestBody HubManagerRegisterRequestDto request);

    @GetMapping("/{hubId}/active")
    boolean checkHubActive(@PathVariable("hubId") UUID hubId);
}