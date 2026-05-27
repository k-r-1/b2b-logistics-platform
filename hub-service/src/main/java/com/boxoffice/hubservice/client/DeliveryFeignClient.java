package com.boxoffice.hubservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "delivery-service")
public interface DeliveryFeignClient {

    @GetMapping("/internal/deliveries/active-count")
    int countActiveDeliveries(@RequestParam UUID hubId);
}
