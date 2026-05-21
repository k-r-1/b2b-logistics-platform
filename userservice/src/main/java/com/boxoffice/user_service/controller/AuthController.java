package com.boxoffice.user_service.controller;

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
}