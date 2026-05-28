package com.boxoffice.userservice.client;

import com.boxoffice.userservice.client.dto.HubManagerRegisterRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubServiceClient {

    @PatchMapping("/internal/hubs/{hubId}/manager")
    void registerHubManager(@PathVariable("hubId") UUID hubId,
                            @RequestBody HubManagerRegisterRequestDto request);
}