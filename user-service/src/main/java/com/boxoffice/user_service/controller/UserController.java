package com.boxoffice.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getUserList() {
        // 아직 DB 연동 전이므로, 토큰이 통과했을 때 보여줄 임시 데이터(Mock Data)를 반환합니다.
        List<Map<String, String>> mockUsers = List.of(
                Map.of("id", "1", "username", "user1", "name", "홍길동", "status", "인증 파이프라인 개통 완료! 🎉")
        );

        return ResponseEntity.ok(mockUsers);
    }
}