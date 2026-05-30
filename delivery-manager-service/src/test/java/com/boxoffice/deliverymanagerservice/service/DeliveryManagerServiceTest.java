package com.boxoffice.deliverymanagerservice.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.deliverymanagerservice.client.HubClient;
import com.boxoffice.deliverymanagerservice.dto.*;
import com.boxoffice.deliverymanagerservice.entity.DeliveryManager;
import com.boxoffice.deliverymanagerservice.entity.DeliveryType;
import com.boxoffice.deliverymanagerservice.entity.ManagerStatus;
import com.boxoffice.deliverymanagerservice.exception.DeliveryManagerErrorCode;
import com.boxoffice.deliverymanagerservice.kafka.DeliveryNotificationProducer;
import com.boxoffice.deliverymanagerservice.repository.DeliveryManagerRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerServiceTest {

    @InjectMocks
    private DeliveryManagerService deliveryManagerService;

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @Mock
    private HubClient hubClient;

    @Mock
    private DeliveryNotificationProducer notificationProducer;

    // 공통 테스트 데이터
    private DeliveryManager testManager;
    private final UUID managerId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID hubId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testManager = DeliveryManager.builder()
                .userId(userId)
                .hubId(hubId)
                .type(DeliveryType.HUB_TO_HUB)
                .status(ManagerStatus.WAITING)
                .build();
        // BaseEntity 의 ID 강제 주입
        ReflectionTestUtils.setField(testManager, "id", managerId);
    }

    // ================= [ 생성 (Create) 테스트 ] ================= //

    @Test
    @DisplayName("배송 담당자 생성 성공 - 허브가 정상 상태일 때")
    void createDeliveryManager_Success() {
        // given
        DeliveryManagerCreateRequestDto request = new DeliveryManagerCreateRequestDto();
        ReflectionTestUtils.setField(request, "userId", userId);
        ReflectionTestUtils.setField(request, "hubId", hubId);
        ReflectionTestUtils.setField(request, "type", DeliveryType.HUB_TO_HUB);

        when(hubClient.checkHubActive(hubId)).thenReturn(true);
        when(deliveryManagerRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(deliveryManagerRepository.save(any(DeliveryManager.class))).thenReturn(testManager);

        // when
        DeliveryManagerResponseDto response = deliveryManagerService.createDeliveryManager(request, "MASTER");

        // then
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        verify(hubClient, times(1)).checkHubActive(hubId);
        verify(deliveryManagerRepository, times(1)).save(any(DeliveryManager.class));
    }

    @Test
    @DisplayName("배송 담당자 생성 실패 - 허브가 비활성화 상태일 때")
    void createDeliveryManager_Fail_HubInactive() {
        // given
        DeliveryManagerCreateRequestDto request = new DeliveryManagerCreateRequestDto();
        ReflectionTestUtils.setField(request, "hubId", hubId);

        when(hubClient.checkHubActive(hubId)).thenReturn(false);

        // when & then
        BaseException ex = assertThrows(BaseException.class,
                () -> deliveryManagerService.createDeliveryManager(request, "MASTER"));
        assertEquals(DeliveryManagerErrorCode.HUB_IS_NOT_ACTIVE, ex.getErrorCode());
        verify(deliveryManagerRepository, never()).save(any(DeliveryManager.class));
    }

    @Test
    @DisplayName("배송 담당자 생성 실패 - 권한 없음 (DELIVERY_MANAGER가 시도)")
    void createDeliveryManager_Fail_Forbidden() {
        // given
        DeliveryManagerCreateRequestDto request = new DeliveryManagerCreateRequestDto();

        // when & then
        BaseException ex = assertThrows(BaseException.class,
                () -> deliveryManagerService.createDeliveryManager(request, "DELIVERY_MANAGER"));
        assertEquals(DeliveryManagerErrorCode.FORBIDDEN_ACCESS, ex.getErrorCode());
    }

    // ================= [ 단건 및 다건 조회 (Read) 테스트 ] ================= //

    @Test
    @DisplayName("배송 담당자 단건 조회 성공 - 본인 조회 허용")
    void getDeliveryManager_Success_Self() {
        // given
        when(deliveryManagerRepository.findById(managerId)).thenReturn(Optional.of(testManager));

        // when (본인의 userId로 접근, 권한은 일반 유저)
        DeliveryManagerResponseDto response = deliveryManagerService.getDeliveryManager(managerId, userId.toString(), "DELIVERY_MANAGER");

        // then
        assertEquals(managerId, response.getId());
    }

    @Test
    @DisplayName("배송 담당자 다건 조회 성공 - HUB_MANAGER 데이터 격리 적용")
    void getDeliveryManagerList_Success_HubManagerIsolation() {
        // given
        DeliveryManagerSearchDto searchDto = new DeliveryManagerSearchDto();
        PageRequest pageable = PageRequest.of(0, 10);
        Page<DeliveryManager> mockPage = new PageImpl<>(List.of(testManager));

        // 검색 허브 아이디가 DTO에 비어있더라도, HUB_MANAGER 권한이므로 강제로 주입되어야 함
        UUID requesterHubId = UUID.randomUUID();

        when(deliveryManagerRepository.searchManagers(eq(requesterHubId), any(), any(), eq(pageable)))
                .thenReturn(mockPage);

        // when
        Page<DeliveryManagerResponseDto> result = deliveryManagerService.getDeliveryManagerList(
                searchDto, pageable, "HUB_MANAGER", requesterHubId.toString()
        );

        // then
        assertEquals(1, result.getTotalElements());
        assertEquals(requesterHubId, searchDto.getHubId()); // 🌟 핵심 검증: 헤더의 HubId가 DTO에 강제 주입되었는가?
        verify(deliveryManagerRepository, times(1)).searchManagers(eq(requesterHubId), any(), any(), eq(pageable));
    }

    // ================= [ 수정 및 삭제 (Update & Delete) 테스트 ] ================= //

    @Test
    @DisplayName("배송 담당자 정보 수정 성공 - 소속 허브 변경 시 유효성 검사 수행")
    void updateDeliveryManager_Success() {
        // given
        DeliveryManagerUpdateRequestDto request = new DeliveryManagerUpdateRequestDto();
        UUID newHubId = UUID.randomUUID();
        ReflectionTestUtils.setField(request, "hubId", newHubId);
        ReflectionTestUtils.setField(request, "type", DeliveryType.HUB_TO_COMPANY);

        when(deliveryManagerRepository.findById(managerId)).thenReturn(Optional.of(testManager));
        when(hubClient.checkHubActive(newHubId)).thenReturn(true);

        // when
        DeliveryManagerResponseDto response = deliveryManagerService.updateDeliveryManager(managerId, request, "MASTER");

        // then
        assertEquals(newHubId, testManager.getHubId());
        assertEquals(DeliveryType.HUB_TO_COMPANY, testManager.getType());
        verify(hubClient, times(1)).checkHubActive(newHubId); // 허브 검증 수행 확인
    }

    @Test
    @DisplayName("배송 담당자 삭제 성공 (Soft Delete)")
    void deleteDeliveryManager_Success() {
        // given
        when(deliveryManagerRepository.findById(managerId)).thenReturn(Optional.of(testManager));
        UUID requesterId = UUID.randomUUID();

        // when
        deliveryManagerService.deleteDeliveryManager(managerId, requesterId.toString(), "MASTER");

        // then
        assertNotNull(testManager.getDeletedAt());
        assertEquals(requesterId, testManager.getDeletedBy());
    }

    // ================= [ 배정 및 Kafka 알림 (Assign & Produce) 테스트 ] ================= //

    @Test
    @DisplayName("기사님 자동 배정 및 카프카 이벤트 발행 성공")
    void assignNextDeliveryManager_Success() {
        // given
        DeliveryAssignRequestDto request = new DeliveryAssignRequestDto();
        ReflectionTestUtils.setField(request, "hubId", hubId);
        ReflectionTestUtils.setField(request, "type", DeliveryType.HUB_TO_HUB);
        ReflectionTestUtils.setField(request, "deliveryId", "DELIVERY-123");

        // OrderInfo DTO 수동 조립 (NPE 방지)
        DeliveryAssignRequestDto.OrderInfo orderInfo = new DeliveryAssignRequestDto.OrderInfo();
        ReflectionTestUtils.setField(orderInfo, "orderId", "ORD-123");
        ReflectionTestUtils.setField(orderInfo, "products", new ArrayList<>()); // 빈 리스트 처리
        ReflectionTestUtils.setField(request, "order", orderInfo);

        // RouteInfo DTO 수동 조립
        DeliveryAssignRequestDto.RouteInfo routeInfo = new DeliveryAssignRequestDto.RouteInfo();
        ReflectionTestUtils.setField(routeInfo, "origin", "서울허브");
        ReflectionTestUtils.setField(request, "route", routeInfo);

        when(deliveryManagerRepository.findFirstByHubIdAndTypeAndStatusAndDeletedAtIsNullOrderByLastAssignedAtAsc(
                hubId, DeliveryType.HUB_TO_HUB, ManagerStatus.WAITING)).thenReturn(Optional.of(testManager));

        // Kafka Event 전송은 void 메서드이므로 Mockito의 doNothing(기본값)이 동작

        // when
        DeliveryAssignResponseDto response = deliveryManagerService.assignNextDeliveryManager(request);

        // then
        assertEquals(managerId, response.getDeliveryManagerId());
        assertNotNull(testManager.getLastAssignedAt()); // 배정 시각이 기록되었는지 확인

        // 카프카 프로듀서가 1번 정상 호출되었는지 검증!
        verify(notificationProducer, times(1)).sendDeliveryAssignedEvent(any());
    }
}