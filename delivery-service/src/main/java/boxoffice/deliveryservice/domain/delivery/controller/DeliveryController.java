package boxoffice.deliveryservice.domain.delivery.controller;

import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryStatusUpdateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryUpdateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.response.DeliveryResponseDto;
import boxoffice.deliveryservice.domain.delivery.service.DeliveryService;
import boxoffice.deliveryservice.domain.deliveryroute.dto.request.DeliveryRouteStatusUpdateRequestDto;
import boxoffice.deliveryservice.domain.deliveryroute.dto.request.DeliveryRouteUpdateRequestDto;
import boxoffice.deliveryservice.domain.deliveryroute.dto.response.DeliveryRouteResponseDto;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DeliveryResponseDto>>> getDeliveries(
            @RequestHeader("X-User-Id") String keycloakSub,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.getDeliveries(keycloakSub, pageable)));
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<DeliveryResponseDto>> getDelivery(
            @RequestHeader("X-User-Id") String keycloakSub,
            @PathVariable UUID deliveryId) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.getDelivery(keycloakSub, deliveryId)));
    }

    @GetMapping("/{deliveryId}/routes")
    public ResponseEntity<ApiResponse<PageResponse<DeliveryRouteResponseDto>>> getDeliveryRoutes(
            @RequestHeader("X-User-Id") String keycloakSub,
            @PathVariable UUID deliveryId,
            @PageableDefault(sort = "sequence", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(deliveryService.getDeliveryRoutes(keycloakSub, deliveryId, pageable)));
    }

    @GetMapping("/{deliveryId}/routes/{routeId}")
    public ResponseEntity<ApiResponse<DeliveryRouteResponseDto>> getDeliveryRoute(
            @RequestHeader("X-User-Id") String keycloakSub,
            @PathVariable UUID deliveryId,
            @PathVariable UUID routeId) {
        return ResponseEntity.ok(
                ApiResponse.success(deliveryService.getDeliveryRoute(keycloakSub, deliveryId, routeId)));
    }

    @PatchMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<DeliveryResponseDto>> updateDelivery(
            @RequestHeader("X-User-Id") String keycloakSub,
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.updateDelivery(keycloakSub, deliveryId, request)));
    }

    @PatchMapping("/{deliveryId}/status")
    public ResponseEntity<ApiResponse<DeliveryResponseDto>> updateDeliveryStatus(
            @RequestHeader("X-User-Id") String keycloakSub,
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryStatusUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.updateDeliveryStatus(keycloakSub, deliveryId, request)));
    }

    @PatchMapping("/{deliveryId}/routes/{routeId}")
    public ResponseEntity<ApiResponse<DeliveryRouteResponseDto>> updateDeliveryRoute(
            @RequestHeader("X-User-Id") String keycloakSub,
            @PathVariable UUID deliveryId,
            @PathVariable UUID routeId,
            @Valid @RequestBody DeliveryRouteUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.updateDeliveryRoute(keycloakSub, deliveryId, routeId, request)));
    }

    @PatchMapping("/{deliveryId}/routes/{routeId}/status")
    public ResponseEntity<ApiResponse<DeliveryRouteResponseDto>> updateDeliveryRouteStatus(
            @RequestHeader("X-User-Id") String keycloakSub,
            @PathVariable UUID deliveryId,
            @PathVariable UUID routeId,
            @Valid @RequestBody DeliveryRouteStatusUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.updateDeliveryRouteStatus(keycloakSub, deliveryId, routeId, request)));
    }

    @DeleteMapping("/{deliveryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDelivery(
            @RequestHeader("X-User-Id") String keycloakSub,
            @PathVariable UUID deliveryId) {
        deliveryService.deleteDelivery(keycloakSub, deliveryId);
    }

    @DeleteMapping("/{deliveryId}/routes/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeliveryRoute(
            @RequestHeader("X-User-Id") String keycloakSub,
            @PathVariable UUID deliveryId,
            @PathVariable UUID routeId) {
        deliveryService.deleteDeliveryRoute(keycloakSub, deliveryId, routeId);
    }
}