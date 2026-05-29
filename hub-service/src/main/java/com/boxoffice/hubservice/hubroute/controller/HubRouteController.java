package com.boxoffice.hubservice.hubroute.controller;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.hubservice.hubroute.dto.request.HubRouteCreateRequestDto;
import com.boxoffice.hubservice.hubroute.dto.request.HubRouteUpdateRequestDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteCreateResponseDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteGetResponseDto;
import com.boxoffice.hubservice.hubroute.service.HubRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "HubRoute", description = "허브 경로 관리 API")
@RestController
@RequestMapping("/api/v1/hub-routes")
@RequiredArgsConstructor
public class HubRouteController {

    private final HubRouteService hubRouteService;

    @Operation(summary = "허브 경로 생성", description = "두 허브 간의 경로를 생성합니다. MASTER 권한 필요.")
    @PostMapping
    public ResponseEntity<ApiResponse<HubRouteCreateResponseDto>> createHubRoute(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody HubRouteCreateRequestDto request
    ) {
        validateMasterRole(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, hubRouteService.createHubRoute(request)));
    }

    @Operation(summary = "허브 경로 단건 조회", description = "허브 경로 ID로 단건 조회합니다.")
    @GetMapping("/{routeId}")
    public ResponseEntity<ApiResponse<HubRouteGetResponseDto>> getHubRoute(
            @PathVariable UUID routeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hubRouteService.getHubRoute(routeId)));
    }

    @Operation(summary = "허브 경로 목록 조회", description = "허브 경로 목록을 동적 필터로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HubRouteGetResponseDto>>> getHubRoutes(
            @Parameter(description = "출발 허브 ID") @RequestParam(required = false) UUID originHubId,
            @Parameter(description = "도착 허브 ID") @RequestParam(required = false) UUID destinationHubId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hubRouteService.getHubRoutes(originHubId, destinationHubId, page, size)));
    }

    @Operation(summary = "허브 경로 수정", description = "허브 경로의 소요 시간 또는 거리를 수정합니다. MASTER 권한 필요.")
    @PatchMapping("/{routeId}")
    public ResponseEntity<ApiResponse<HubRouteGetResponseDto>> updateHubRoute(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID routeId,
            @Valid @RequestBody HubRouteUpdateRequestDto request
    ) {
        validateMasterRole(role);
        return ResponseEntity.ok(ApiResponse.success(hubRouteService.updateHubRoute(routeId, request)));
    }

    @Operation(summary = "허브 경로 삭제", description = "허브 경로를 삭제합니다. MASTER 권한 필요.")
    @DeleteMapping("/{routeId}")
    public ResponseEntity<Void> deleteHubRoute(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID routeId
    ) {
        validateMasterRole(role);
        hubRouteService.deleteHubRoute(routeId);
        return ResponseEntity.noContent().build();
    }

    private void validateMasterRole(String role) {
        if (!"MASTER".equals(role)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }
}
