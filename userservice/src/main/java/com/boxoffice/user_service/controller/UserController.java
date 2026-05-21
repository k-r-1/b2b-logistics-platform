package com.boxoffice.user_service.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.user_service.dto.UserResponseDto;
import com.boxoffice.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

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

        // 팀원의 공통 규격인 ApiResponse.success()를 사용하여 이쁘게 래핑
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponseDto>>> getUserList(
            @RequestHeader("X-User-Id") String requesterId, // 🌟 문지기가 꽂아준 명찰 받기!
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("[Controller] 사용자 목록 조회 요청. RequesterId: {}, Page: {}", requesterId, pageable.getPageNumber());

        // 서비스로 요청자 ID(requesterId)를 같이 넘겨줍니다.
        Page<UserResponseDto> responseDtoPage = userService.getUserList(requesterId, pageable);

        return ResponseEntity.ok(ApiResponse.success(responseDtoPage));
    }
}