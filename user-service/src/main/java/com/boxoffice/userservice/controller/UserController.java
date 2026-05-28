package com.boxoffice.userservice.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.userservice.dto.UserHubUpdateRequestDto;
import com.boxoffice.userservice.dto.UserResponseDto;
import com.boxoffice.userservice.dto.UserStatusUpdateRequestDto;
import com.boxoffice.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "사용자 외부 관리 API (User External)", description = "사용자 정보 조회, 상태 변경, 삭제 등 클라이언트/어드민 노출용 API")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "내 정보 조회",
            description = "Gateway가 전파한 X-User-Id 헤더 기반으로 현재 로그인한 사용자의 상세 정보를 조회합니다." // 🌟 설명문 수정
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyInfo(
            @RequestHeader("X-User-Id") String keycloakSub) {
        UserResponseDto response = userService.getMyInfo(keycloakSub);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "사용자 전체 목록 조회 (권한별 격리)",
            description = "MASTER 권한은 전체 유저를, HUB_MANAGER 권한은 본인 소속 허브의 유저 목록만 페이지네이션하여 조회할 수 있습니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponseDto>>> getUserList(
            @RequestHeader("X-User-Id") String keycloakSub,
            Pageable pageable) {
        Page<UserResponseDto> response = userService.getUserList(keycloakSub, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "사용자 가입 승인 및 상태 변경",
            description = "PENDING 상태의 사용자를 승인(APPROVED)하거나 상태를 변경합니다. HUB_MANAGER는 소속 허브 유저만 승인 가능하며, 승인 완료 시 Hub Service로 연동됩니다."
    )
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUserStatus(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-User-Id") String keycloakSub,
            @Valid @RequestBody UserStatusUpdateRequestDto requestDto) {
        UserResponseDto response = userService.updateUserStatus(userId, keycloakSub, requestDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "사용자 탈퇴 및 삭제 (Soft Delete)",
            description = "MASTER 권한 전용 API로, 대상 유저의 상태를 DELETED로 변경하고 논리적 삭제 처리를 수행합니다."
    )
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-User-Id") String keycloakSub) {
        userService.deleteUser(userId, keycloakSub);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "허브 관리자 소속 허브 수동 변경",
            description = "운영팀(MASTER)이 Admin UI에서 특정 허브 관리자 유저의 소속 허브 ID를 강제로 변경 재배정할 때 사용합니다."
    )
    @PatchMapping("/{userId}/hub")
    public ResponseEntity<ApiResponse<Void>> updateUserHub(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UserHubUpdateRequestDto request,
            @RequestHeader("X-User-Role") String role) {

        log.info("[Controller] 허브 관리자 소속 허브 변경 요청 수신. UserId: {}, NewHubId: {}", userId, request.getHubId());
        userService.updateUserHub(userId, request.getHubId(), role);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}