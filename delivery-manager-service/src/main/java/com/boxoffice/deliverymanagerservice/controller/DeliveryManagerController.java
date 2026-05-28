package com.boxoffice.deliverymanagerservice.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.deliverymanagerservice.dto.*;
import com.boxoffice.deliverymanagerservice.service.DeliveryManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/delivery-managers")
@RequiredArgsConstructor
@Tag(name = "배송 담당자 외부 관리 API (Delivery Manager External)", description = "배송 담당자(기사) 생성, 조회, 수정, 삭제 등 클라이언트 및 어드민 노출용 API")
public class DeliveryManagerController {

    private final DeliveryManagerService deliveryManagerService;

    @Operation(
            summary = "배송 담당자 생성",
            description = "신규 배송 담당자를 등록합니다. 등록 시 초기 상태는 WAITING(대기)으로 설정됩니다. (MASTER 또는 HUB_MANAGER 권한 필요)"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryManagerResponseDto>> createDeliveryManager(
            @Valid @RequestBody DeliveryManagerCreateRequestDto request,
            @RequestHeader("X-User-Role") String role) {
        DeliveryManagerResponseDto response = deliveryManagerService.createDeliveryManager(request, role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "배송 담당자 단건 조회",
            description = "특정 배송 담당자의 상세 정보를 조회합니다. 관리자(MASTER, HUB_MANAGER)이거나 조회 대상이 본인(OWNER)인 경우에만 접근이 허용됩니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryManagerResponseDto>> getDeliveryManager(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role) {
        DeliveryManagerResponseDto response = deliveryManagerService.getDeliveryManager(id, userId, role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "배송 담당자 정보 수정",
            description = "특정 배송 담당자의 소속 허브(hubId) 또는 배송 유형(type)을 수정합니다. (MASTER 또는 HUB_MANAGER 권한 필요)"
    )
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryManagerResponseDto>> updateDeliveryManager(
            @PathVariable UUID id,
            @Valid @RequestBody DeliveryManagerUpdateRequestDto request,
            @RequestHeader("X-User-Role") String role) {
        DeliveryManagerResponseDto response = deliveryManagerService.updateDeliveryManager(id, request, role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "배송 담당자 삭제 (Soft Delete)",
            description = "배송 담당자를 논리적 삭제(Soft Delete) 처리합니다. (MASTER 또는 HUB_MANAGER 권한 필요)"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDeliveryManager(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String requesterId,
            @RequestHeader("X-User-Role") String role) {
        deliveryManagerService.deleteDeliveryManager(id, requesterId, role);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}