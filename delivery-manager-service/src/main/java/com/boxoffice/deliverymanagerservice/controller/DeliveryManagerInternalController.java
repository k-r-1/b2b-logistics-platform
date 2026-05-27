package com.boxoffice.deliverymanagerservice.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.deliverymanagerservice.dto.DeliveryAssignRequestDto;
import com.boxoffice.deliverymanagerservice.dto.DeliveryAssignResponseDto;
import com.boxoffice.deliverymanagerservice.service.DeliveryManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/internal/v1/delivery-managers")
@RequiredArgsConstructor
@Tag(name = "배송 담당자 내부 통신 API (Delivery Manager Internal)", description = "[서버 간 통신 전용] 타 마이크로서비스에서 FeignClient를 통해 호출하는 내부 전용 API 세트")
public class DeliveryManagerInternalController {

    private final DeliveryManagerService deliveryManagerService;

    @Operation(
            summary = "[Internal] 허브 삭제 시 연관 기사 초기화 및 상태 변경 (안전망)",
            description = "Hub Service에서 특정 허브가 폐쇄/삭제(Soft Delete)될 때 호출됩니다. 해당 허브에 소속된 기사들의 hubId를 일괄 null 처리하고, 유령 상태 방지를 위해 상태를 WAITING(대기 중)으로 강제 전환합니다."
    )
    @PatchMapping("/clear-hub/{hubId}")
    public ResponseEntity<ApiResponse<Void>> clearDeliveryManagerHubId(
            @PathVariable("hubId") UUID hubId) {

        log.info("[Internal Controller] 허브 삭제에 따른 기사님 hubId 초기화 요청 수신. TargetHubId: {}", hubId);
        deliveryManagerService.clearDeliveryManagerHubId(hubId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "[Internal] 라운드 로빈 기사 자동 배정",
            description = "배송(Delivery) 생성 시 호출됩니다. 조건(해당 허브, 배송 타입, WAITING 상태)에 맞는 기사 중 가장 오래 쉰(lastAssignedAt 기준 오름차순) 기사를 찾아 자동 배정하고 배정 시간을 최신화합니다."
    )
    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<DeliveryAssignResponseDto>> assignNextManager(
            @RequestBody DeliveryAssignRequestDto request) {

        log.info("[Internal Controller] 배달 담당자 자동 배정 요청 수신. HubId: {}", request.getHubId());
        DeliveryAssignResponseDto response = deliveryManagerService.assignNextDeliveryManager(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}