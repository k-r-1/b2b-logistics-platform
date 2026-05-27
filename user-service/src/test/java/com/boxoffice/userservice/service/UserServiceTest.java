package com.boxoffice.userservice.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.userservice.client.KeycloakClient;
import com.boxoffice.userservice.dto.UserResponseDto;
import com.boxoffice.userservice.dto.UserStatusUpdateRequestDto;
import com.boxoffice.userservice.entity.Email;
import com.boxoffice.userservice.entity.User;
import com.boxoffice.userservice.entity.UserRole;
import com.boxoffice.userservice.entity.UserStatus;
import com.boxoffice.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakClient keycloakClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private User masterUser;
    private User hubManagerUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "realm", "test-realm");
        ReflectionTestUtils.setField(userService, "adminUsername", "admin");
        ReflectionTestUtils.setField(userService, "adminPassword", "admin123");
        ReflectionTestUtils.setField(userService, "adminClientId", "admin-cli");
        ReflectionTestUtils.setField(userService, "userClientId", "boxoffice-app");

        // 테스트에 사용할 더미(Dummy) 엔티티 생성
        masterUser = User.builder()
                .keycloakSub("master-sub")
                .email(new Email("master@test.com"))
                .name("마스터")
                .role(UserRole.MASTER)
                .build();
        ReflectionTestUtils.setField(masterUser, "id", UUID.randomUUID());

        hubManagerUser = User.builder()
                .keycloakSub("hub-manager-sub")
                .email(new Email("hub@test.com"))
                .name("허브매니저")
                .role(UserRole.HUB_MANAGER)
                .hubId("hub-1")
                .build();
        ReflectionTestUtils.setField(hubManagerUser, "id", UUID.randomUUID());

        targetUser = User.builder()
                .keycloakSub("target-sub")
                .email(new Email("target@test.com"))
                .name("신규유저")
                .role(UserRole.HUB_MANAGER)
                .hubId("hub-1")
                .status(UserStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(targetUser, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("내 정보 조회 성공 테스트")
    void getMyInfo_Success() {
        // given (상황 세팅: DB에서 유저를 찾으면 masterUser를 반환한다)
        given(userRepository.findByKeycloakSub("master-sub")).willReturn(Optional.of(masterUser));

        // when (실제 메서드 실행)
        UserResponseDto result = userService.getMyInfo("master-sub");

        // then (결과 검증)
        assertNotNull(result);
        assertEquals("마스터", result.getName());
        assertEquals("master@test.com", result.getEmail());
    }

    @Test
    @DisplayName("사용자 목록 조회 - MASTER 권한 (전체 조회 성공)")
    void getUserList_Master_Success() {
        // given
        given(userRepository.findByKeycloakSub("master-sub")).willReturn(Optional.of(masterUser));

        Page<User> mockPage = new PageImpl<>(List.of(hubManagerUser, targetUser));
        given(userRepository.findAll(any(PageRequest.class))).willReturn(mockPage);

        // when
        Page<UserResponseDto> result = userService.getUserList("master-sub", PageRequest.of(0, 10));

        // then
        assertEquals(2, result.getContent().size());
        verify(userRepository, times(1)).findAll(any(PageRequest.class)); // findAll이 호출되었는지 검증
    }

    @Test
    @DisplayName("가입 승인 - HUB_MANAGER가 같은 허브 유저 승인 (성공)")
    void updateUserStatus_HubManager_Success() {
        // given
        UserStatusUpdateRequestDto requestDto = new UserStatusUpdateRequestDto();
        ReflectionTestUtils.setField(requestDto, "status", "APPROVED");

        given(userRepository.findByKeycloakSub("hub-manager-sub")).willReturn(Optional.of(hubManagerUser));
        given(userRepository.findById(targetUser.getId())).willReturn(Optional.of(targetUser));

        // when
        UserResponseDto result = userService.updateUserStatus(targetUser.getId(), "hub-manager-sub", requestDto);

        // then
        assertEquals(UserStatus.APPROVED.name(), result.getStatus());
        assertEquals(UserStatus.APPROVED, targetUser.getStatus()); // 실제 엔티티 상태도 변경되었는지 확인
    }

    @Test
    @DisplayName("가입 승인 - HUB_MANAGER가 다른 허브 유저 승인 시도 (실패 - FORBIDDEN)")
    void updateUserStatus_HubManager_DifferentHub_Fail() {
        // given
        UserStatusUpdateRequestDto requestDto = new UserStatusUpdateRequestDto();
        ReflectionTestUtils.setField(requestDto, "status", "APPROVED");

        // 다른 허브(hub-2) 소속의 매니저 생성
        User otherHubManager = User.builder().role(UserRole.HUB_MANAGER).hubId("hub-2").build();

        given(userRepository.findByKeycloakSub("other-hub-sub")).willReturn(Optional.of(otherHubManager));
        given(userRepository.findById(targetUser.getId())).willReturn(Optional.of(targetUser)); // target은 hub-1

        // when & then
        BaseException exception = assertThrows(BaseException.class, () ->
                userService.updateUserStatus(targetUser.getId(), "other-hub-sub", requestDto)
        );
        assertEquals(CommonErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("사용자 삭제 - MASTER 권한 (Soft Delete 성공)")
    void deleteUser_Master_Success() {
        // given
        given(userRepository.findByKeycloakSub("master-sub")).willReturn(Optional.of(masterUser));
        given(userRepository.findById(targetUser.getId())).willReturn(Optional.of(targetUser));

        // when
        userService.deleteUser(targetUser.getId(), "master-sub");

        // then
        assertEquals(UserStatus.DELETED, targetUser.getStatus()); // 상태가 DELETED로 바뀌었는지 검증
        assertNotNull(targetUser.getDeletedAt()); // Soft Delete 시간이 찍혔는지 검증
        assertEquals(masterUser.getId(), targetUser.getDeletedBy()); // 삭제자가 마스터로 기록되었는지 검증
    }

    @Test
    @DisplayName("로그아웃 - 유효한 토큰 Redis 등록 성공")
    void logout_Success() {
        // given
        // 유효시간(exp)이 1시간 뒤인 가짜 JWT 토큰 생성
        String header = Base64.getUrlEncoder().encodeToString("{}".getBytes());
        long exp = (System.currentTimeMillis() / 1000) + 3600;
        String payload = Base64.getUrlEncoder().encodeToString(("{\"exp\":" + exp + "}").getBytes());
        String sig = Base64.getUrlEncoder().encodeToString("sig".getBytes());
        String fakeToken = header + "." + payload + "." + sig;
        String authHeader = "Bearer " + fakeToken;

        // RedisTemplate의 opsForValue()가 호출될 때 Mock 객체를 반환하도록 설정
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        userService.logout(authHeader);

        // then
        // valueOperations.set(...) 메서드가 정확한 인자와 함께 1번 호출되었는지 검증
        verify(valueOperations, times(1)).set(
                eq(fakeToken),
                eq("logout"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        );
    }
}