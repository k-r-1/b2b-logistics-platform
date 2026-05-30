package com.boxoffice.companyservice.company.client;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.company.client.fallback.UserClientFallbackFactory;
import com.boxoffice.companyservice.company.client.dto.UserCompanyUpdateRequestDto;
import com.boxoffice.companyservice.company.client.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/internal/v1/users", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @PatchMapping("/{userId}/company")
    ApiResponse<Void> updateUserCompany(
            @PathVariable("userId") UUID userId,
            @RequestBody UserCompanyUpdateRequestDto request
    );

    @GetMapping("/keycloak/{keycloakSub}")
    ApiResponse<UserResponseDto> getUserByKeycloakSub(@PathVariable("keycloakSub") String keycloakSub);
}
