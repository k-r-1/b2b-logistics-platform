package com.boxoffice.userservice.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.userservice.dto.UserResponseDto;
import com.boxoffice.userservice.dto.UserStatusUpdateRequestDto;
import com.boxoffice.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
//: 내 정보 조회, 사용자 목록 검색, 회원 탈퇴 등 관리
public class UserController {

    private final UserService userService;

//    @GetMapping
//    public ResponseEntity<List<Map<String, String>>> getUserList() {
//        List<Map<String, String>> mockUsers = List.of(
//                Map.of("id", "1", "username", "user1", "name", "홍길동", "status", "인증 파이프라인 개통 완료! 🎉")
//        );
//
//        return ResponseEntity.ok(mockUsers);
//    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyInfo(
            @RequestHeader("X-User-Id") String userId) {

        log.info("[Controller] 내 정보 조회 요청. 전달받은 UserId: {}", userId);

        UserResponseDto responseDto = userService.getMyInfo(userId);

        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponseDto>>> getUserList(
            @RequestHeader("X-User-Id") String requesterId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("[Controller] 사용자 목록 조회 요청. RequesterId: {}, Page: {}", requesterId, pageable.getPageNumber());

        // 서비스로 요청자 ID(requesterId)를 같이 넘겨줍니다.
        Page<UserResponseDto> responseDtoPage = userService.getUserList(requesterId, pageable);

        return ResponseEntity.ok(ApiResponse.success(responseDtoPage));
    }

    // 🌟 가입 승인/거절 API
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUserStatus(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String requesterId,
            @RequestBody UserStatusUpdateRequestDto request) {

        log.info("[Controller] 유저 상태 변경 요청. TargetId: {}, RequesterId: {}, NewStatus: {}", id, requesterId, request.getStatus());

        UserResponseDto responseDto = userService.updateUserStatus(id, requesterId, request);

        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    // 🌟 사용자 삭제 API
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String requesterId) {

        log.info("[Controller] 유저 삭제 요청. TargetId: {}, RequesterId: {}", id, requesterId);

        userService.deleteUser(id, requesterId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "[Internal] 유저 단건 조회 (FeignClient 통신용)",
            description = "타 마이크로서비스에서 유저의 companyId 등을 조회하기 위해 사용하는 내부 서버 간 통신 전용 API입니다. (프론트엔드 호출 금지)"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable UUID id) {
        log.info("[Internal Controller] 타 서비스로부터 유저 조회 요청 수신. UserId: {}", id);
        UserResponseDto response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "[Internal] Keycloak Sub 기반 유저 조회 (FeignClient 전용)",
            description = "타 서비스가 Gateway에서 전파받은 X-User-Id 헤더(Keycloak Sub)를 그대로 사용하여 유저 정보와 companyId를 조회하는 API입니다."
    )
    @GetMapping("/keycloak/{keycloakSub}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserBySub(@PathVariable String keycloakSub) {
        log.info("[Internal Controller] Keycloak Sub 기반 유저 조회 요청 수신. Sub: {}", keycloakSub);
        UserResponseDto response = userService.getUserBySub(keycloakSub);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}