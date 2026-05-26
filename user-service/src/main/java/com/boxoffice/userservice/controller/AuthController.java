package com.boxoffice.userservice.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.userservice.dto.UserLoginRequestDto;
import com.boxoffice.userservice.dto.UserSignupRequestDto;
import com.boxoffice.userservice.service.UserService;
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
@RequestMapping("/api/v1/auth") // 🌟 auth 경로는 여기로 통일!
@RequiredArgsConstructor
//로그인, 회원가입, 로그아웃 등 '인증(Auth)'과 관련
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody UserSignupRequestDto request) {
        log.info("[Controller] 회원가입 요청 수신. Username: {}, Role: {}", request.getUsername(), request.getRole());

        userService.signUp(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("회원가입 신청이 PENDING 상태로 정상 접수되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody UserLoginRequestDto request) {

        String accessToken = userService.login(request);

        Map<String, String> responseData = new HashMap<>();
        responseData.put("accessToken", accessToken);

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    // 🌟 사용자 로그아웃 API
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            // Gateway가 넘겨주는 원래의 Authorization 헤더 전체를 받습니다.
            @RequestHeader("Authorization") String authHeader) {

        log.info("[Controller] 로그아웃 요청.");

        userService.logout(authHeader);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}