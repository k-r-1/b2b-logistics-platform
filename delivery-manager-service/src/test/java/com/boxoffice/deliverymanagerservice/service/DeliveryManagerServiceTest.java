package com.boxoffice.deliverymanagerservice.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.deliverymanagerservice.dto.DeliveryAssignRequestDto;
import com.boxoffice.deliverymanagerservice.dto.DeliveryAssignResponseDto;
import com.boxoffice.deliverymanagerservice.dto.DeliveryManagerCreateRequestDto;
import com.boxoffice.deliverymanagerservice.dto.DeliveryManagerResponseDto;
import com.boxoffice.deliverymanagerservice.entity.DeliveryManager;
import com.boxoffice.deliverymanagerservice.entity.DeliveryType;
import com.boxoffice.deliverymanagerservice.entity.ManagerStatus;
import com.boxoffice.deliverymanagerservice.exception.DeliveryManagerErrorCode;
import com.boxoffice.deliverymanagerservice.repository.DeliveryManagerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Mockito 환경에서 실행 (가짜 객체 주입)
class DeliveryManagerServiceTest {

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @InjectMocks
    private DeliveryManagerService deliveryManagerService;

    // 테스트용 공통 더미 데이터
    private final UUID managerId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID hubId = UUID.randomUUID();

    @Test
    @DisplayName("배송 담당자 생성 성공 - MASTER 권한")
    void createDeliveryManager_Success() {
        // Given (준비)
        DeliveryManagerCreateRequestDto request = mock(DeliveryManagerCreateRequestDto.class);
        when(request.getUserId()).thenReturn(userId);
        when(request.getHubId()).thenReturn(hubId);
        when(request.getType()).thenReturn(DeliveryType.HUB_TO_HUB);

        // 중복 회원이 없는 상황 가정
        when(deliveryManagerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When (실행)
        DeliveryManagerResponseDto response = deliveryManagerService.createDeliveryManager(request, "MASTER");

        // Then (검증)
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(hubId, response.getHubId());

        // save 메서드가 1번 호출되었는지 검증
        verify(deliveryManagerRepository, times(1)).save(any(DeliveryManager.class));
    }

    @Test
    @DisplayName("배송 담당자 생성 실패 - 이미 등록된 유저")
    void createDeliveryManager_Fail_AlreadyRegistered() {
        // Given
        DeliveryManagerCreateRequestDto request = mock(DeliveryManagerCreateRequestDto.class);
        when(request.getUserId()).thenReturn(userId);

        DeliveryManager existingManager = DeliveryManager.builder().userId(userId).build();
        when(deliveryManagerRepository.findByUserId(userId)).thenReturn(Optional.of(existingManager));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () ->
                deliveryManagerService.createDeliveryManager(request, "MASTER"));

        assertEquals(DeliveryManagerErrorCode.ALREADY_REGISTERED_MANAGER, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 담당자 단건 조회 성공 - 본인(OWNER) 요청")
    void getDeliveryManager_Success_ByOwner() {
        // Given
        DeliveryManager manager = DeliveryManager.builder()
                .userId(userId)
                .hubId(hubId)
                .type(DeliveryType.HUB_TO_COMPANY)
                .slackId("TEST_SLACK")
                .status(ManagerStatus.WAITING)
                .build();

        when(deliveryManagerRepository.findById(managerId)).thenReturn(Optional.of(manager));

        // When (본인 userId로 요청)
        DeliveryManagerResponseDto response = deliveryManagerService.getDeliveryManager(managerId, userId.toString(), "USER");

        // Then
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
    }

    @Test
    @DisplayName("배송 담당자 단건 조회 실패 - 권한 없음 (남의 정보 조회)")
    void getDeliveryManager_Fail_Forbidden() {
        // Given
        DeliveryManager manager = DeliveryManager.builder()
                .userId(userId) // 실제 주인의 ID
                .build();
        when(deliveryManagerRepository.findById(managerId)).thenReturn(Optional.of(manager));

        String otherUserId = UUID.randomUUID().toString(); // 남의 ID

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () ->
                deliveryManagerService.getDeliveryManager(managerId, otherUserId, "USER"));

        assertEquals(DeliveryManagerErrorCode.FORBIDDEN_ACCESS, exception.getErrorCode());
    }

    @Test
    @DisplayName("내부 API: 라운드 로빈 기사 자동 배정 성공")
    void assignNextDeliveryManager_Success() {
        // Given
        DeliveryAssignRequestDto request = mock(DeliveryAssignRequestDto.class);
        when(request.getHubId()).thenReturn(hubId);
        when(request.getType()).thenReturn(DeliveryType.HUB_TO_HUB);

        DeliveryManager manager = spy(DeliveryManager.builder()
                .userId(userId)
                .hubId(hubId)
                .type(DeliveryType.HUB_TO_HUB)
                .slackId("TEST")
                .status(ManagerStatus.WAITING)
                .build()); // id가 null이므로 아래에서 별도 처리, 여기선 객체 생성을 확인

        when(deliveryManagerRepository.findFirstByHubIdAndTypeAndIsDeletedFalseOrderByLastAssignedAtAsc(hubId, DeliveryType.HUB_TO_HUB))
                .thenReturn(Optional.of(manager));

        // When
        DeliveryAssignResponseDto response = deliveryManagerService.assignNextDeliveryManager(request);

        // Then
        assertNotNull(response);
        verify(manager, times(1)).recordAssignment(); // 마지막 배정 시간 최신화가 호출되었는지 검증
    }
}