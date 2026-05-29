package com.boxoffice.userservice.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.userservice.dto.UserLoginRequestDto;
import com.boxoffice.userservice.dto.UserSignupRequestDto;
import com.boxoffice.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API (Auth)", description = "로그인, 회원가입, 로그아웃 등 '인증(Auth)'과 관련된 API")
public class AuthController {

    private final UserService userService;

    @Operation(
            summary = "사용자 회원가입",
            description = "새로운 사용자가 회원가입을 신청합니다. 신청 직후 상태는 PENDING(대기)이며 관리자의 승인이 필요합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signUp(@Valid @RequestBody UserSignupRequestDto request) {
        log.info("[Controller] 회원가입 요청 수신. Username: {}, Role: {}", request.getUsername(), request.getRole());

        userService.signUp(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "회원가입 신청이 PENDING 상태로 정상 접수되었습니다."));
    }

    @Operation(
            summary = "사용자 로그인 및 토큰 발급",
            description = "Keycloak 자격 증명을 사용하여 로그인을 진행하고 Access Token을 발급받습니다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody UserLoginRequestDto request) {

        String accessToken = userService.login(request);

        Map<String, String> responseData = new HashMap<>();
        responseData.put("accessToken", accessToken);

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @Operation(
            summary = "사용자 로그아웃 (블랙리스트 등록)",
            description = "Gateway가 넘겨주는 원래의 Authorization 헤더 전체를 받아 토큰을 Redis 블랙리스트에 등록하여 무효화합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {

        log.info("[Controller] 로그아웃 요청.");

        userService.logout(authHeader);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}