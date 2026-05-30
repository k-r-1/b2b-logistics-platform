package com.boxoffice.hubservice.hub.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.hubservice.hub.dto.request.HubAssignManagerRequestDto;
import com.boxoffice.hubservice.hub.dto.response.HubActiveResponseDto;
import com.boxoffice.hubservice.hub.dto.response.HubGetResponseDto;
import com.boxoffice.hubservice.hub.service.HubService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Hidden
@RestController
@RequestMapping("/internal/v1/hubs")
@RequiredArgsConstructor
public class HubInternalController {

    private final HubService hubService;

    @GetMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubGetResponseDto>> getHub(
            @PathVariable UUID hubId
    ) {
        return ResponseEntity.ok(ApiResponse.success(hubService.getHub(hubId)));
    }

    @GetMapping("/{hubId}/active")
    public ResponseEntity<ApiResponse<HubActiveResponseDto>> getActiveHub(@PathVariable UUID hubId) {
        return ResponseEntity.ok(ApiResponse.success(hubService.getActiveHub(hubId)));
    }

    @PatchMapping("/{hubId}/manager")
    public ResponseEntity<ApiResponse<HubGetResponseDto>> assignManager(
            @PathVariable UUID hubId,
            @Valid @RequestBody HubAssignManagerRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hubService.assignManager(hubId, request.managerId())));
    }
}

