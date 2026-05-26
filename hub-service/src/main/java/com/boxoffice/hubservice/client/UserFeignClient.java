package com.boxoffice.hubservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    @PatchMapping("/internal/v1/users/clear-hub/{hubId}")
    void clearHub(@PathVariable UUID hubId);
}
