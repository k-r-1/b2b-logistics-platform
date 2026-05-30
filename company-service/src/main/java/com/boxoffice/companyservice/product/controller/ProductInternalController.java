package com.boxoffice.companyservice.product.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.product.dto.request.HubStockCountRequestDto;
import com.boxoffice.companyservice.product.dto.response.HubStockCountResponseDto;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
