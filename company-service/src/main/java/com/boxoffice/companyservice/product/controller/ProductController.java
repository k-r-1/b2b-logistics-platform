package com.boxoffice.companyservice.product.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.product.dto.request.ProductCreateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import com.boxoffice.companyservice.product.service.ProductFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies/{companyId}/products")
@Tag(name = "Product", description = "상품 API")
public class ProductController {

    private final ProductFacade productFacade;

    @Operation(summary = "상품 생성", description = "업체 하위에 상품을 생성합니다. 상품 관리 허브는 소속 업체의 hubId를 기준으로 검증합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductCreateResponseDto>> createProduct(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakSub,
            @PathVariable("companyId") UUID companyId,
            @Valid @RequestBody ProductCreateRequestDto request
    ) {
        ProductCreateResponseDto response = productFacade.createProduct(
                companyId,
                request,
                userRole,
                userHubId,
                keycloakSub
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response));
    }
}
