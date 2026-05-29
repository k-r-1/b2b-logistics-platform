package com.boxoffice.hubservice.hubroute.controller;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.hubservice.hubroute.dto.request.HubRouteCreateRequestDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteCreateResponseDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteGetResponseDto;
import com.boxoffice.hubservice.hubroute.service.HubRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @Valid
            @RequestBody HubRouteCreateRequestDto request
    ) {
        validateMasterRole(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, hubRouteService.createHubRoute(request)));
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<ApiResponse<HubRouteGetResponseDto>> getHubRoute(
            @PathVariable UUID routeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hubRouteService.getHubRoute(routeId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HubRouteGetResponseDto>>> getHubRoutes(
            @RequestParam(required = false) UUID originHubId,
            @RequestParam(required = false) UUID destinationHubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hubRouteService.getHubRoutes(originHubId, destinationHubId, page, size)));
    }

    private void validateMasterRole(String role) {
        if (!"MASTER".equals(role)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }
}
