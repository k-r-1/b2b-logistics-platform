package com.boxoffice.deliverymanagerservice.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.deliverymanagerservice.dto.*;
import com.boxoffice.deliverymanagerservice.service.DeliveryManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/delivery-managers")
@RequiredArgsConstructor
public class DeliveryManagerController {

    private final DeliveryManagerService deliveryManagerService;

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryManagerResponseDto>> createDeliveryManager(
            @RequestBody DeliveryManagerCreateRequestDto request,
            @RequestHeader("X-User-Role") String role) {
        DeliveryManagerResponseDto response = deliveryManagerService.createDeliveryManager(request, role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryManagerResponseDto>> getDeliveryManager(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role) {
        DeliveryManagerResponseDto response = deliveryManagerService.getDeliveryManager(id, userId, role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryManagerResponseDto>> updateDeliveryManager(
            @PathVariable UUID id,
            @RequestBody DeliveryManagerUpdateRequestDto request,
            @RequestHeader("X-User-Role") String role) {
        DeliveryManagerResponseDto response = deliveryManagerService.updateDeliveryManager(id, request, role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDeliveryManager(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String requesterId,
            @RequestHeader("X-User-Role") String role) {
        deliveryManagerService.deleteDeliveryManager(id, requesterId, role);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/internal/assign")
    public ResponseEntity<ApiResponse<DeliveryAssignResponseDto>> assignNextManager(
            @RequestBody DeliveryAssignRequestDto request) {

        log.info("[Internal Controller] 배송 담당자 자동 배정 요청 수신. HubId: {}, Type: {}", request.getHubId(), request.getType());
        DeliveryAssignResponseDto response = deliveryManagerService.assignNextDeliveryManager(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @PatchMapping("/internal/clear-hub/{hubId}")
    public ResponseEntity<ApiResponse<Void>> clearDeliveryManagerHubId(
            @PathVariable("hubId") UUID hubId) {

        log.info("[Internal Controller] 허브 삭제에 따른 기사님 hubId 초기화 요청 수신. TargetHubId: {}", hubId);

        deliveryManagerService.clearDeliveryManagerHubId(hubId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}