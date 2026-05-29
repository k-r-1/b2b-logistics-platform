package com.boxoffice.userservice.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.userservice.dto.UserCompanyUpdateRequestDto;
import com.boxoffice.userservice.dto.UserResponseDto;
import com.boxoffice.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
@Tag(name = "사용자 내부 통신 API (User Internal)", description = "[서버 간 통신 전용] 타 마이크로서비스에서 FeignClient를 통해 호출하는 내부 전용 API 세트")
public class UserInternalController {

    private final UserService userService;

    @Operation(
            summary = "[Internal] 유저 식별자 기반 단건 조회 (회사 ID 포함)",
            description = "이 API는 프론트엔드 호출용이 아니며, 주문 서비스 등 타 마이크로서비스에서 FeignClient를 통해 유저의 소속 회사(companyId) 정보 등을 조회할 때 사용합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserInternal(
            @PathVariable("id") UUID id) {

        log.info("[Internal Controller] 유저 단건 조회 요청 수신. UserId: {}", id);

        UserResponseDto response = userService.getUserById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "[Internal] Keycloak Sub 기반 유저 조회 (FeignClient 전용)",
            description = "타 서비스가 Gateway에서 전파받은 X-User-Id 헤더(Keycloak Sub)를 그대로 사용하여 유저 정보와 companyId를 조회하는 API입니다."
    )
    @GetMapping("/keycloak/{keycloak_sub}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserByKeycloakSubInternal(
            @PathVariable("keycloak_sub") String keycloakSub) {

        log.info("[Internal Controller] Keycloak Sub 기반 유저 조회 요청 수신. Sub: {}", keycloakSub);

        UserResponseDto response = userService.getUserBySub(keycloakSub);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "[Internal] 업체 담당자 회사 ID 매pping 부여",
            description = "업체(Company) 서비스에서 신규 업체를 생성하거나 수정할 때, 해당 업체의 담당자(SUPPLIER_MANAGER)에게 companyId를 부여하기 위해 호출하는 내부 통신용 API입니다."
    )
    @PatchMapping("/{userId}/company")
    public ResponseEntity<ApiResponse<Void>> updateUserCompanyInternal(
            @PathVariable("userId") UUID userId,
            @RequestBody UserCompanyUpdateRequestDto request) {

        log.info("[Internal Controller] 업체 담당자 회사 ID 지정 요청 수신. UserId: {}, CompanyId: {}", userId, request.getCompanyId());

        userService.updateUserCompany(userId, request);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "[Internal] 허브 삭제 시 연관 유저 hub_id 일괄 초기화 (안전망)",
            description = "Hub Service에서 특정 허브를 Soft Delete 할 때 연쇄적으로 호출되며, 해당 허브에 묶여있던 유저들의 hubId를 일괄적으로 null 처리하는 데이터 정합성 보장용 API입니다."
    )
    @PatchMapping("/clear-hub/{hubId}")
    public ResponseEntity<ApiResponse<Void>> clearUserHubId(
            @PathVariable("hubId") UUID hubId) {

        log.info("[Internal Controller] 허브 삭제에 따른 유저 hubId 일괄 초기화 요청 수신. TargetHubId: {}", hubId);

        userService.clearUserHubId(hubId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}