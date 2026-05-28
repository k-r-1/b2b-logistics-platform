package com.boxoffice.userservice.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.userservice.client.HubServiceClient;
import com.boxoffice.userservice.client.KeycloakClient;
import com.boxoffice.userservice.dto.UserResponseDto;
import com.boxoffice.userservice.dto.UserStatusUpdateRequestDto;
import com.boxoffice.userservice.entity.Email;
import com.boxoffice.userservice.entity.User;
import com.boxoffice.userservice.entity.UserRole;
import com.boxoffice.userservice.entity.UserStatus;
import com.boxoffice.userservice.exception.UserErrorCode;
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

    @Mock
    private HubServiceClient hubServiceClient;

    private User masterUser;
    private User hubManagerUser;
    private User targetUser;

    // 🌟 타입 불일치 에러 해결을 위해 UUID로 통일
    private UUID hubId1;
    private UUID hubId2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "realm", "test-realm");
        ReflectionTestUtils.setField(userService, "adminUsername", "admin");
        ReflectionTestUtils.setField(userService, "adminPassword", "admin123");
        ReflectionTestUtils.setField(userService, "adminClientId", "admin-cli");
        ReflectionTestUtils.setField(userService, "userClientId", "boxoffice-app");

        hubId1 = UUID.randomUUID();
        hubId2 = UUID.randomUUID();

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
                .hubId(hubId1) // 🌟 String -> UUID로 변경
                .build();
        ReflectionTestUtils.setField(hubManagerUser, "id", UUID.randomUUID());

        targetUser = User.builder()
                .keycloakSub("target-sub")
                .email(new Email("target@test.com"))
                .name("신규유저")
                .role(UserRole.HUB_MANAGER)
                .hubId(hubId1) // 🌟 String -> UUID로 변경
                .status(UserStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(targetUser, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("내 정보 조회 성공 테스트")
    void getMyInfo_Success() {
        given(userRepository.findByKeycloakSub("master-sub")).willReturn(Optional.of(masterUser));
        UserResponseDto result = userService.getMyInfo("master-sub");
        assertNotNull(result);
        assertEquals("마스터", result.getName());
        assertEquals("master@test.com", result.getEmail());
    }

    @Test
    @DisplayName("사용자 목록 조회 - MASTER 권한 (전체 조회 성공)")
    void getUserList_Master_Success() {
        given(userRepository.findByKeycloakSub("master-sub")).willReturn(Optional.of(masterUser));
        Page<User> mockPage = new PageImpl<>(List.of(hubManagerUser, targetUser));
        given(userRepository.findAll(any(PageRequest.class))).willReturn(mockPage);

        Page<UserResponseDto> result = userService.getUserList("master-sub", PageRequest.of(0, 10));

        assertEquals(2, result.getContent().size());
        verify(userRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("가입 승인 - HUB_MANAGER가 같은 허브 유저 승인 (성공)")
    void updateUserStatus_HubManager_Success() {
        UserStatusUpdateRequestDto requestDto = new UserStatusUpdateRequestDto();
        ReflectionTestUtils.setField(requestDto, "status", "APPROVED");

        given(userRepository.findByKeycloakSub("hub-manager-sub")).willReturn(Optional.of(hubManagerUser));
        given(userRepository.findById(targetUser.getId())).willReturn(Optional.of(targetUser));

        // 🌟 리뷰 반영: FeignClient 호출이 발생할 때 아무것도 하지 않고(doNothing) 통과하도록 설정하여 NPE 방지
        doNothing().when(hubServiceClient).registerHubManager(any(), any());

        UserResponseDto result = userService.updateUserStatus(targetUser.getId(), "hub-manager-sub", requestDto);

        assertEquals(UserStatus.APPROVED.name(), result.getStatus());
        assertEquals(UserStatus.APPROVED, targetUser.getStatus());

        // 추가 검증: 실제로 FeignClient가 1번 호출되었는지 확인
        verify(hubServiceClient, times(1)).registerHubManager(any(), any());
    }

    @Test
    @DisplayName("가입 승인 - HUB_MANAGER가 다른 허브 유저 승인 시도 (실패 - FORBIDDEN)")
    void updateUserStatus_HubManager_DifferentHub_Fail() {
        UserStatusUpdateRequestDto requestDto = new UserStatusUpdateRequestDto();
        ReflectionTestUtils.setField(requestDto, "status", "APPROVED");

        // 다른 허브(hubId2) 소속의 매니저 생성 시 UUID 적용
        User otherHubManager = User.builder().role(UserRole.HUB_MANAGER).hubId(hubId2).build();

        given(userRepository.findByKeycloakSub("other-hub-sub")).willReturn(Optional.of(otherHubManager));
        given(userRepository.findById(targetUser.getId())).willReturn(Optional.of(targetUser));

        BaseException exception = assertThrows(BaseException.class, () ->
                userService.updateUserStatus(targetUser.getId(), "other-hub-sub", requestDto)
        );
        assertEquals(CommonErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("사용자 삭제 - MASTER 권한 (Soft Delete 성공)")
    void deleteUser_Master_Success() {
        given(userRepository.findByKeycloakSub("master-sub")).willReturn(Optional.of(masterUser));
        given(userRepository.findById(targetUser.getId())).willReturn(Optional.of(targetUser));

        userService.deleteUser(targetUser.getId(), "master-sub");

        assertEquals(UserStatus.DELETED, targetUser.getStatus());
        assertNotNull(targetUser.getDeletedAt());
    }

    @Test
    @DisplayName("로그아웃 - 유효한 토큰 Redis 등록 성공")
    void logout_Success() {
        String header = Base64.getUrlEncoder().encodeToString("{}".getBytes());
        long exp = (System.currentTimeMillis() / 1000) + 3600;
        String payload = Base64.getUrlEncoder().encodeToString(("{\"exp\":" + exp + "}").getBytes());
        String sig = Base64.getUrlEncoder().encodeToString("sig".getBytes());
        String fakeToken = header + "." + payload + "." + sig;
        String authHeader = "Bearer " + fakeToken;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        userService.logout(authHeader);

        verify(valueOperations, times(1)).set(eq(fakeToken), eq("logout"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    // =====================================================================
    // 🌟 허브 폐쇄/변경 안전망 관련 테스트 로직
    // =====================================================================

    @Test
    @DisplayName("허브 관리자 소속 허브 수동 변경 성공 - MASTER 권한")
    void updateUserHub_Success() {
        UUID newHubId = UUID.randomUUID();
        given(userRepository.findById(targetUser.getId())).willReturn(Optional.of(targetUser));

        userService.updateUserHub(targetUser.getId(), newHubId, "MASTER");

        // 상태 변경(updateHub) 도메인 로직이 잘 작동하여 값이 바뀌었는지 검증
        assertEquals(newHubId, targetUser.getHubId());
    }

    @Test
    @DisplayName("허브 관리자 소속 허브 수동 변경 실패 - 권한 없음")
    void updateUserHub_Fail_Forbidden() {
        UUID newHubId = UUID.randomUUID();

        // HUB_MANAGER 권한으로 찌르면 FORBIDDEN 예외가 터져야 함
        BaseException exception = assertThrows(BaseException.class, () ->
                userService.updateUserHub(targetUser.getId(), newHubId, "HUB_MANAGER"));

        assertEquals(UserErrorCode.FORBIDDEN_ACCESS, exception.getErrorCode());
    }

    @Test
    @DisplayName("내부 API: 허브 삭제 시 연관 유저 hubId 일괄 초기화 성공")
    void clearUserHubId_Success() {
        UUID targetHubId = UUID.randomUUID();

        userService.clearUserHubId(targetHubId);

        // 레포지토리의 벌크 연산 메서드가 정확히 1번 호출되었는지 검증
        verify(userRepository, times(1)).clearHubIdByHubId(targetHubId);
    }
}