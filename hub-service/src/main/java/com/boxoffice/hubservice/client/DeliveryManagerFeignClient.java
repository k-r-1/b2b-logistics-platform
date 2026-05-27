package com.boxoffice.hubservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "delivery-manager-service")
public interface DeliveryManagerFeignClient {

    @PatchMapping("/internal/delivery-managers/clear-hub/{hubId}")
    void clearHub(@PathVariable UUID hubId);
}
