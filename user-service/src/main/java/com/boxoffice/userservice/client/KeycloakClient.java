package com.boxoffice.userservice.client;

import com.boxoffice.userservice.client.dto.KeycloakUserCreateRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "keycloak-client", url = "${keycloak.url:http://localhost:8089}")
public interface KeycloakClient {

    @PostMapping(value = "/realms/{realm}/protocol/openid-connect/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Map<String, Object> getAdminToken(
            @PathVariable("realm") String realm,
            Map<String, ?> formParams
    );

    @PostMapping("/admin/realms/{realm}/users")
    ResponseEntity<Void> createUser(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable("realm") String realm,
            @RequestBody KeycloakUserCreateRequestDto request
    );
}