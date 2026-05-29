package com.boxoffice.deliverymanagerservice.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.deliverymanagerservice.dto.*;
import com.boxoffice.deliverymanagerservice.entity.DeliveryManager;
import com.boxoffice.deliverymanagerservice.entity.ManagerStatus;
import com.boxoffice.deliverymanagerservice.exception.DeliveryManagerErrorCode;
import com.boxoffice.deliverymanagerservice.repository.DeliveryManagerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryManagerService {

    private final DeliveryManagerRepository deliveryManagerRepository;

    @Transactional
    public DeliveryManagerResponseDto createDeliveryManager(DeliveryManagerCreateRequestDto request, String role) {
        checkAdminRole(role);

        if (deliveryManagerRepository.findByUserId(request.getUserId()).isPresent()) {
            log.warn("[DeliveryManagerCreate] 중복 등록 시도. UserId: {}", request.getUserId());
            throw new BaseException(DeliveryManagerErrorCode.ALREADY_REGISTERED_MANAGER);
        }

        DeliveryManager newManager = DeliveryManager.builder()
                .userId(request.getUserId())
                .hubId(request.getHubId())
                .type(request.getType())
                .slackId(null)
                .status(ManagerStatus.WAITING)
                .build();

        deliveryManagerRepository.save(newManager);
        return DeliveryManagerResponseDto.from(newManager);
    }


    @Transactional(readOnly = true)
    public DeliveryManagerResponseDto getDeliveryManager(UUID managerId, String requesterId, String role) {
        DeliveryManager manager = deliveryManagerRepository.findById(managerId)
                .orElseThrow(() -> new BaseException(DeliveryManagerErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        if (!"MASTER".equals(role) && !"HUB_MANAGER".equals(role)) {
            if (!manager.getUserId().equals(UUID.fromString(requesterId))) {
                throw new BaseException(DeliveryManagerErrorCode.FORBIDDEN_ACCESS);
            }
        }

        return DeliveryManagerResponseDto.from(manager);
    }

    @Transactional
    public DeliveryManagerResponseDto updateDeliveryManager(UUID managerId, DeliveryManagerUpdateRequestDto request, String role) {
        checkAdminRole(role);

        DeliveryManager manager = deliveryManagerRepository.findById(managerId)
                .orElseThrow(() -> new BaseException(DeliveryManagerErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        if (request.getHubId() != null) manager.updateHub(request.getHubId());
        if (request.getType() != null) manager.updateType(request.getType());

        return DeliveryManagerResponseDto.from(manager);
    }

    @Transactional
    public void deleteDeliveryManager(UUID managerId, String requesterId, String role) {
        checkAdminRole(role);

        DeliveryManager manager = deliveryManagerRepository.findById(managerId)
                .orElseThrow(() -> new BaseException(DeliveryManagerErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        manager.softDelete(UUID.fromString(requesterId));
    }

    @Transactional
    public DeliveryAssignResponseDto assignNextDeliveryManager(DeliveryAssignRequestDto request) {
        DeliveryManager manager = deliveryManagerRepository
                .findFirstByHubIdAndTypeAndStatusAndDeletedAtIsNullOrderByLastAssignedAtAsc(
                        request.getHubId(), request.getType(), ManagerStatus.WAITING)
                .orElseThrow(() -> new BaseException(DeliveryManagerErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        manager.recordAssignment();
        log.info("[DeliveryManagerAssign] 기사님 자동 배정 완료. ManagerId: {}, HubId: {}", manager.getId(), request.getHubId());

        return new DeliveryAssignResponseDto(manager.getId());
    }

    private void checkAdminRole(String role) {
        if (!"MASTER".equals(role) && !"HUB_MANAGER".equals(role)) {
            throw new BaseException(DeliveryManagerErrorCode.FORBIDDEN_ACCESS);
        }
    }

    @Transactional
    public void clearDeliveryManagerHubId(UUID hubId) {
        log.info("[DeliveryManagerHubClear] 허브 삭제에 따른 기사님 hubId 및 상태 일괄 초기화 시작. TargetHubId: {}", hubId);

        deliveryManagerRepository.clearHubIdAndChangeStatusByHubId(hubId, ManagerStatus.WAITING);

        log.info("[DeliveryManagerHubClear] 기사님 일괄 초기화 완료. TargetHubId: {}", hubId);
    }
}