package com.boxoffice.companyservice.product.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.companyservice.product.dto.request.HubStockCountRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductStockDeductRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductStockItemRequestDto;
import com.boxoffice.companyservice.product.dto.response.HubStockCountResponseDto;
import com.boxoffice.companyservice.product.dto.response.ProductStockCheckResponseDto;
import com.boxoffice.companyservice.product.dto.response.ProductStockDeductResponseDto;
import com.boxoffice.companyservice.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Product Internal", description = "상품 내부 연동 API")
@RequestMapping("/internal/v1/products")
public class ProductInternalController {

    private final ProductService productService;

    @Operation(summary = "허브별 재고 수량 합계 조회", description = "여러 hubId 기준 Product.stockQuantity 합계를 한 번에 조회합니다.")
    @PostMapping("/hubs/stock-counts")
    public ResponseEntity<ApiResponse<List<HubStockCountResponseDto>>> getHubStockCounts(
            @Valid @RequestBody HubStockCountRequestDto request
    ) {
        List<HubStockCountResponseDto> response = productService.getHubStockCounts(request.getHubIds());
        log.info("Internal hub stock counts requested. hubCount={}", request.getHubIds().size());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 재고 주문 가능 여부 확인", description = "주문 전 상품 존재 여부, 삭제 여부, 재고 수량을 확인합니다. 재고 선점은 수행하지 않습니다.")
    @PostMapping("/stocks/check")
    public ResponseEntity<ApiResponse<ProductStockCheckResponseDto>> checkStocks(
            @Valid @RequestBody List<ProductStockItemRequestDto> request
    ) {
        ProductStockCheckResponseDto response = productService.checkStocks(request);
        log.info("Internal stock check requested. itemCount={}", request.size());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 재고 차감", description = "주문 ID 기준 멱등성을 보장하며 상품 재고를 비관적 락으로 차감합니다.")
    @PostMapping("/stocks/deduct")
    public ResponseEntity<ApiResponse<ProductStockDeductResponseDto>> deductStocks(
            @RequestParam(value = "orderId", required = false) UUID orderId,
            @Valid @RequestBody ProductStockDeductRequestDto request
    ) {
        validateOrderId(orderId);
        ProductStockDeductResponseDto response = productService.deductStocks(orderId, request);
        log.info("Internal stock deduct requested. orderId={}, itemCount={}", orderId, request.getProducts().size());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 재고 복원", description = "주문 실패 보상 트랜잭션에서 주문 ID 기준으로 차감된 재고를 복원합니다.")
    @PostMapping("/stocks/restore")
    public ResponseEntity<ApiResponse<Void>> restoreStocks(
            @RequestParam(value = "orderId", required = false) UUID orderId,
            @Valid @RequestBody List<ProductStockItemRequestDto> request
    ) {
        validateOrderId(orderId);
        productService.restoreStocks(orderId, request);
        log.info("Internal stock restore requested. orderId={}, itemCount={}", orderId, request.size());

        return ResponseEntity.ok(ApiResponse.success());
    }

    private void validateOrderId(UUID orderId) {
        if (orderId == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }
}
