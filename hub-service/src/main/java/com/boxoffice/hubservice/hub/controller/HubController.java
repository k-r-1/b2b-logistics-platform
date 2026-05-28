package com.boxoffice.hubservice.hub.controller;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.hubservice.hub.dto.request.HubCreateRequestDto;
import com.boxoffice.hubservice.hub.dto.request.HubClosingRequestDto;
import com.boxoffice.hubservice.hub.dto.request.HubUpdateRequestDto;
import com.boxoffice.hubservice.hub.dto.response.HubCreateResponseDto;
import com.boxoffice.hubservice.hub.dto.response.HubDeactivateResponseDto;
import com.boxoffice.hubservice.hub.dto.response.HubGetResponseDto;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hub.service.HubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Hub", description = "허브 관리 API")
@RestController
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubService hubService;

    @Operation(summary = "허브 생성", description = "새로운 허브를 생성합니다. MASTER 권한 필요.")
    @PostMapping
    public ResponseEntity<ApiResponse<HubCreateResponseDto>> createHub(
            @RequestHeader("X-User-Role") String role,
            @Valid
            @RequestBody HubCreateRequestDto request
    ) {
        validateMasterRole(role);
        HubCreateResponseDto response = hubService.createHub(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response));
    }

    @Operation(summary = "허브 단건 조회", description = "허브 ID로 허브 정보를 조회합니다.")
    @GetMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubGetResponseDto>> getHub(
            @PathVariable UUID hubId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hubService.getHub(hubId)));
    }

    @Operation(summary = "허브 목록 조회", description = "허브 목록을 페이지네이션으로 조회합니다. 이름, 허브 타입으로 필터링 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HubGetResponseDto>>> getHubs(
            @Parameter(description = "허브 이름 (부분 검색)") @RequestParam(required = false) String name,
            @Parameter(description = "허브 타입 (CENTRAL, REGIONAL, CLOSING, INACTIVE)") @RequestParam(required = false) HubType hubType,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(hubService.getHubs(name, hubType, page, size)));
    }

    @Operation(summary = "허브 수정", description = "허브 정보를 수정합니다. MASTER 권한 필요.")
    @PatchMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubGetResponseDto>> updateHub(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID hubId,
            @Valid
            @RequestBody HubUpdateRequestDto request
    ) {
        validateMasterRole(role);
        return ResponseEntity.ok(ApiResponse.success(hubService.updateHub(hubId, request)));
    }

    @Operation(summary = "허브 폐쇄 시작", description = "허브를 CLOSING 상태로 변경합니다. MASTER 권한 필요. CENTRAL 허브는 폐쇄 불가.")
    @PatchMapping("/{hubId}/close")
    public ResponseEntity<ApiResponse<HubGetResponseDto>> closeHub(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID hubId,
            @Valid @RequestBody HubClosingRequestDto request
    ) {
        validateMasterRole(role);
        return ResponseEntity.ok(ApiResponse.success(hubService.startClosingHub(hubId, request)));
    }

    @Operation(summary = "허브 비활성화", description = "CLOSING 상태의 허브를 INACTIVE로 변경합니다. MASTER 권한 필요. CENTRAL 허브는 비활성화 불가.")
    @PatchMapping("/{hubId}/deactivate")
    public ResponseEntity<ApiResponse<HubDeactivateResponseDto>> deactivateHub(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID hubId
    ) {
        validateMasterRole(role);
        return ResponseEntity.ok(ApiResponse.success(hubService.deactivateHub(hubId)));
    }

    private void validateMasterRole(String role) {
        if (!"MASTER".equals(role)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }
}
