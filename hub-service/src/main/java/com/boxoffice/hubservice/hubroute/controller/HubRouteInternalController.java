package com.boxoffice.hubservice.hubroute.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.hubservice.hubroute.dto.response.HubRoutePathResponseDto;
import com.boxoffice.hubservice.hubroute.service.HubRouteService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Hidden
@RestController
@RequestMapping("/internal/v1/hub-routes")
@RequiredArgsConstructor
public class HubRouteInternalController {

    private final HubRouteService hubRouteService;

    @GetMapping("/path")
    public ResponseEntity<ApiResponse<HubRoutePathResponseDto>> calculatePath(
            @RequestParam UUID originHubId,
            @RequestParam UUID destinationHubId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                hubRouteService.calculatePath(originHubId, destinationHubId)));
    }
}
