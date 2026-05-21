package com.boxoffice.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

// URL은 application.yml 환경변수로 관리하되, 기본값은 우리 도커 포트(8089)로 설정합니다.
@FeignClient(name = "keycloak-client", url = "${keycloak.url:http://localhost:8089}")
public interface KeycloakClient {

    /**
     * 🌟 1. Keycloak 어드민 권한을 얻기 위한 마스터 토큰 발급 API
     * 소비 형식을 application/x-www-form-urlencoded 로 지정해야 Keycloak이 알아먹습니다.
     */
    @PostMapping(value = "/realms/{realm}/protocol/openid-connect/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Map<String, Object> getAdminToken(
            @PathVariable("realm") String realm,
            Map<String, ?> formParams
    );

    /**
     * 🌟 2. Keycloak 리얼름에 실제 유저 계정을 생성하는 API
     * 헤더에 1번에서 받아온 Admin Access Token을 'Bearer ...' 형태로 찔러넣어야 승인됩니다.
     */
    @PostMapping("/admin/realms/{realm}/users")
    ResponseEntity<Void> createUser(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable("realm") String realm,
            @RequestBody KeycloakUserCreateRequestDto request
    );
}