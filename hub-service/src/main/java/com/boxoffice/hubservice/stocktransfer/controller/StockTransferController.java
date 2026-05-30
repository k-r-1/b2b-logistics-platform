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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "StockTransfer", description = "재고 이전 API")
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class StockTransferController {

    private final StockTransferService stockTransferService;

    @Operation(summary = "재고 이전 계획 조회", description = "INACTIVE 상태 허브의 재고 이전 계획을 조회합니다. MASTER 권한 필요.")
    @GetMapping("/plan")
    public ResponseEntity<ApiResponse<TransferPlanResponseDto>> getTransferPlan(
            @RequestHeader("X-User-Role") String role,
            @Parameter(description = "이전 출발 허브 ID") @RequestParam UUID fromHubId
    ) {
        requireMaster(role);
        return ResponseEntity.ok(ApiResponse.success(stockTransferService.getTransferPlan(fromHubId)));
    }

    @Operation(summary = "재고 이전 생성", description = "INACTIVE 상태 허브의 재고 이전을 생성합니다. MASTER 권한 필요.")
    @PostMapping
    public ResponseEntity<ApiResponse<List<StockTransferResponseDto>>> createTransfer(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody StockTransferCreateRequestDto request
    ) {
        requireMaster(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, stockTransferService.createTransfer(request)));
    }

    @Operation(
            summary = "재고 이전 목록 조회",
            description = "재고 이전 목록을 조회합니다. MASTER는 전체, HUB_MANAGER는 담당 허브, DELIVERY_MANAGER는 본인 배정 건만 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StockTransferResponseDto>>> getTransfers(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID hubId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
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
            requireHeader(hubId);
            return ResponseEntity.ok(ApiResponse.success(
                    stockTransferService.getTransfersByHub(status, hubId, page, size)));
        }
        if ("DELIVERY_MANAGER".equals(role)) {
            requireHeader(userId);
            return ResponseEntity.ok(ApiResponse.success(
                    stockTransferService.getTransfersByDeliveryManager(status, userId, page, size)));
        }
        throw new BaseException(CommonErrorCode.FORBIDDEN);
    }

    @Operation(
            summary = "재고 이전 단건 조회",
            description = "재고 이전 단건을 조회합니다. MASTER는 전체, HUB_MANAGER는 담당 허브, DELIVERY_MANAGER는 본인 배정 건만 조회 가능합니다."
    )
    @GetMapping("/{transferId}")
    public ResponseEntity<ApiResponse<StockTransferResponseDto>> getTransfer(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID hubId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID transferId
    ) {
        if ("MASTER".equals(role)) {
            return ResponseEntity.ok(ApiResponse.success(stockTransferService.getTransfer(transferId)));
        }
        if ("HUB_MANAGER".equals(role)) {
            requireHeader(hubId);
            return ResponseEntity.ok(ApiResponse.success(
                    stockTransferService.getTransferByHub(transferId, hubId)));
        }
        if ("DELIVERY_MANAGER".equals(role)) {
            requireHeader(userId);
            return ResponseEntity.ok(ApiResponse.success(
                    stockTransferService.getTransferByDeliveryManager(transferId, userId)));
        }
        throw new BaseException(CommonErrorCode.FORBIDDEN);
    }

    @Operation(summary = "재고 이전 출발 처리", description = "PENDING 상태의 재고 이전을 IN_PROGRESS로 변경합니다. HUB_MANAGER 권한 필요.")
    @PatchMapping("/{transferId}/dispatch")
    public ResponseEntity<ApiResponse<StockTransferResponseDto>> dispatch(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Hub-Id") UUID hubId,
            @PathVariable UUID transferId
    ) {
        requireHubManager(role);
        return ResponseEntity.ok(ApiResponse.success(
                stockTransferService.dispatch(transferId, hubId)));
    }

    @Operation(
            summary = "재고 이전 완료 처리",
            description = "IN_PROGRESS 상태의 재고 이전을 COMPLETED로 변경합니다. HUB_MANAGER 또는 DELIVERY_MANAGER 권한 필요."
    )
    @PatchMapping("/{transferId}/complete")
    public ResponseEntity<ApiResponse<StockTransferResponseDto>> complete(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID hubId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID transferId,
            @Valid @RequestBody(required = false) StockTransferCompleteRequestDto request
    ) {
        if ("HUB_MANAGER".equals(role)) {
            requireHeader(hubId);
            return ResponseEntity.ok(ApiResponse.success(
                    stockTransferService.complete(transferId, hubId, request)));
        }
        if ("DELIVERY_MANAGER".equals(role)) {
            requireHeader(userId);
            return ResponseEntity.ok(ApiResponse.success(
                    stockTransferService.completeByDeliveryManager(transferId, userId, request)));
        }
        throw new BaseException(CommonErrorCode.FORBIDDEN);
    }

    @Operation(summary = "재고 이전 취소", description = "PENDING 상태의 재고 이전을 취소합니다. MASTER 또는 HUB_MANAGER 권한 필요.")
    @PatchMapping("/{transferId}/cancel")
    public ResponseEntity<ApiResponse<StockTransferResponseDto>> cancel(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID hubId,
            @PathVariable UUID transferId
    ) {
        if ("MASTER".equals(role)) {
            return ResponseEntity.ok(ApiResponse.success(stockTransferService.cancel(transferId)));
        }
        if ("HUB_MANAGER".equals(role)) {
            requireHeader(hubId);
            return ResponseEntity.ok(ApiResponse.success(stockTransferService.cancel(transferId, hubId)));
        }
        throw new BaseException(CommonErrorCode.FORBIDDEN);
    }

    private void requireMaster(String role) {
        if (!"MASTER".equals(role)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void requireHubManager(String role) {
        if (!"HUB_MANAGER".equals(role)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void requireHeader(Object value) {
        if (value == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }
}
