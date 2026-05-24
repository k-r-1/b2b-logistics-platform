package com.boxoffice.companyservice.company.client;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.company.client.dto.UserCompanyUpdateRequestDto;
import com.boxoffice.companyservice.company.client.dto.UserResponseWrapperDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserClient {

    @PatchMapping("/internal/v1/users/{userId}/company")
    ApiResponse<Void> updateUserCompany(
            @PathVariable("userId") UUID userId,
            @RequestBody UserCompanyUpdateRequestDto request
    );

    @GetMapping("/api/v1/users/keycloak/{keycloakSub}")
    UserResponseWrapperDto getUserByKeycloakSub(@PathVariable("keycloakSub") String keycloakSub);
}
