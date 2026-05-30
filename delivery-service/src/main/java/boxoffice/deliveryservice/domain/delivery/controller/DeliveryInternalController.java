package boxoffice.deliveryservice.domain.delivery.controller;

import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.response.DeliveryResponseDto;
import boxoffice.deliveryservice.domain.delivery.service.DeliveryService;
import com.boxoffice.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryInternalController {

    private final DeliveryService deliveryService;

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryResponseDto>> createDelivery(
            @RequestBody @Valid DeliveryCreateRequestDto request) {
        DeliveryResponseDto response = deliveryService.createDelivery(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response));
    }

    @GetMapping("/active-count")
    public ResponseEntity<ApiResponse<Integer>> getActiveDeliveryCount(
            @RequestParam UUID hubId) {
        int count = deliveryService.getActiveDeliveryCount(hubId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
