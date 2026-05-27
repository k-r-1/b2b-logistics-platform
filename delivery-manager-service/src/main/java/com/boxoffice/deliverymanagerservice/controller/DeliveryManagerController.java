package com.boxoffice.deliverymanagerservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/delivery-managers")
public class DeliveryManagerController {

    @GetMapping("/auth-test")
    public String authTest(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        log.info("Gateway 통과 성공! 진입한 유저 ID: {}, 권한: {}", userId, userRole);

        return String.format("🎉 인증 통과 완료! [유저 ID: %s, 권한: %s]", userId, userRole);
    }
}