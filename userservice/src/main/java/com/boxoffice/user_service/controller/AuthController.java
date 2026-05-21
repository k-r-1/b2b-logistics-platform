package com.boxoffice.user_service.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.user_service.dto.UserLoginRequestDto;
import com.boxoffice.user_service.dto.UserSignupRequestDto;
import com.boxoffice.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

        // 1. 서비스에 로그인 위임 및 토큰 받아오기
        String accessToken = userService.login(request);

        // 2. JSON 형태({ "accessToken": "eyJh..." })로 이쁘게 감싸기
        Map<String, String> responseData = new HashMap<>();
        responseData.put("accessToken", accessToken);

        // 3. 200 OK와 함께 공통 규격(ApiResponse)으로 반환
        // (팀원의 ApiResponse에 success() 메서드가 있다고 가정)
        return ResponseEntity.ok(ApiResponse.success(responseData));
    }
}