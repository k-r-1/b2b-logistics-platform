package com.boxoffice.hubservice.stocktransfer.controller;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.hubservice.stocktransfer.dto.request.StockTransferCompleteRequestDto;
import com.boxoffice.hubservice.stocktransfer.dto.request.StockTransferCreateRequestDto;
import com.boxoffice.hubservice.stocktransfer.dto.response.StockTransferResponseDto;
import com.boxoffice.hubservice.stocktransfer.dto.response.TransferPlanResponseDto;
import com.boxoffice.hubservice.stocktransfer.entity.TransferStatus;
import com.boxoffice.hubservice.stocktransfer.service.StockTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class StockTransferController {

    private final StockTransferService stockTransferService;

    @GetMapping("/plan")
    public ResponseEntity<ApiResponse<TransferPlanResponseDto>> getTransferPlan(
            @RequestHeader("X-User-Role") String role,
            @RequestParam UUID fromHubId
    ) {
        if (!"MASTER".equals(role)) throw new BaseException(CommonErrorCode.FORBIDDEN);
        return ResponseEntity.ok(ApiResponse.success(stockTransferService.getTransferPlan(fromHubId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<StockTransferResponseDto>>> createTransfer(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody StockTransferCreateRequestDto request
    ) {
        if (!"MASTER".equals(role)) throw new BaseException(CommonErrorCode.FORBIDDEN);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, stockTransferService.createTransfer(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StockTransferResponseDto>>> getTransfers(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hub-Id", required = false) String hubIdStr,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) UUID fromHubId,
            @RequestParam(required = false) UUID toHubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if ("MASTER".equals(role)) {
            return ResponseEntity.ok(ApiResponse.success(
                    stockTransferService.getTransfers(status, fromHubId, toHubId, page, size)));
        }
        if ("HUB_MANAGER".equals(role)) {
            try {
                UUID hubId = UUID.fromString(hubIdStr);
                return ResponseEntity.ok(ApiResponse.success(
                        stockTransferService.getTransfersByHub(status, hubId, page, size)));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new BaseException(CommonErrorCode.INVALID_INPUT);
            }
        }
        if ("DELIVERY_MANAGER".equals(role)) {
            try {
                UUID deliveryManagerId = UUID.fromString(userIdStr);
                return ResponseEntity.ok(ApiResponse.success(
                        stockTransferService.getTransfersByDeliveryManager(status, deliveryManagerId, page, size)));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new BaseException(CommonErrorCode.INVALID_INPUT);
            }
        }
        throw new BaseException(CommonErrorCode.FORBIDDEN);
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<?> getTransfer(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hub-Id", required = false) String hubIdStr,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @PathVariable UUID transferId
    ) {
        if ("MASTER".equals(role)) {
            return ResponseEntity.ok(ApiResponse.success(stockTransferService.getTransfer(transferId)));
        }
        if ("HUB_MANAGER".equals(role)) {
            try {
                UUID hubId = UUID.fromString(hubIdStr);
                return ResponseEntity.ok(ApiResponse.success(
                        stockTransferService.getTransferByHub(transferId, hubId)));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new BaseException(CommonErrorCode.INVALID_INPUT);
            }
        }
        if ("DELIVERY_MANAGER".equals(role)) {
            try {
                UUID deliveryManagerId = UUID.fromString(userIdStr);
                return ResponseEntity.ok(ApiResponse.success(
                        stockTransferService.getTransferByDeliveryManager(transferId, deliveryManagerId)));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new BaseException(CommonErrorCode.INVALID_INPUT);
            }
        }
        throw new BaseException(CommonErrorCode.FORBIDDEN);
    }

    @PatchMapping("/{transferId}/dispatch")
    public ResponseEntity<ApiResponse<StockTransferResponseDto>> dispatch(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-Hub-Id") String hubIdStr,
            @PathVariable UUID transferId
    ) {
        UUID hubId;
        if (!"HUB_MANAGER".equals(role)) throw new BaseException(CommonErrorCode.FORBIDDEN);
        try {
            hubId = UUID.fromString(hubIdStr);
        } catch (IllegalArgumentException e) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
        return ResponseEntity.ok(ApiResponse.success(
                stockTransferService.dispatch(transferId, hubId)));
    }

    @PatchMapping("/{transferId}/complete")
    public ResponseEntity<ApiResponse<StockTransferResponseDto>> complete(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hub-Id", required = false) String hubIdStr,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @PathVariable UUID transferId,
            @Valid @RequestBody(required = false) StockTransferCompleteRequestDto request
    ) {
        if ("HUB_MANAGER".equals(role)) {
            try {
                UUID hubId = UUID.fromString(hubIdStr);
                return ResponseEntity.ok(ApiResponse.success(
                        stockTransferService.complete(transferId, hubId, request)));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new BaseException(CommonErrorCode.INVALID_INPUT);
            }
        }
        if ("DELIVERY_MANAGER".equals(role)) {
            try {
                UUID deliveryManagerId = UUID.fromString(userIdStr);
                return ResponseEntity.ok(ApiResponse.success(
                        stockTransferService.completeByDeliveryManager(transferId, deliveryManagerId, request)));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new BaseException(CommonErrorCode.INVALID_INPUT);
            }
        }
        throw new BaseException(CommonErrorCode.FORBIDDEN);
    }

    @PatchMapping("/{transferId}/cancel")
    public ResponseEntity<ApiResponse<StockTransferResponseDto>> cancel(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Hub-Id", required = false) String hubIdStr,
            @PathVariable UUID transferId
    ) {
        if ("MASTER".equals(role)) {
            return ResponseEntity.ok(ApiResponse.success(stockTransferService.cancel(transferId)));
        }
        if ("HUB_MANAGER".equals(role)) {
            try {
                UUID hubId = UUID.fromString(hubIdStr);
                return ResponseEntity.ok(ApiResponse.success(stockTransferService.cancel(transferId, hubId)));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new BaseException(CommonErrorCode.INVALID_INPUT);
            }
        }
        throw new BaseException(CommonErrorCode.FORBIDDEN);
    }
}
