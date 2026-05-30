package com.boxoffice.companyservice.product.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.common.util.PageableUtils;
import com.boxoffice.companyservice.product.dto.request.ProductCreateRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductUpdateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import com.boxoffice.companyservice.product.dto.response.ProductResponseDto;
import com.boxoffice.companyservice.product.dto.search.ProductSearchCondition;
import com.boxoffice.companyservice.product.service.ProductFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @Operation(summary = "상품 목록 검색", description = "업체 하위 상품 목록을 조건에 따라 페이지 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDto>>> searchProducts(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakSub,
            @PathVariable("companyId") UUID companyId,
            @ModelAttribute ProductSearchCondition condition,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable validPageable = PageableUtils.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().stream().findFirst().map(Sort.Order::getProperty).orElse("createdAt"),
                pageable.getSort().stream().findFirst().map(Sort.Order::isDescending).orElse(true)
        );

        Page<ProductResponseDto> products = productFacade.searchProducts(
                companyId,
                condition,
                validPageable,
                userRole,
                userHubId,
                keycloakSub
        );
        String sort = validPageable.getSort().stream()
                .findFirst()
                .map(order -> order.getProperty() + "," + order.getDirection())
                .orElse("createdAt,DESC");

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(products, sort)));
    }

    @Operation(summary = "상품 상세 조회", description = "업체 하위 단일 상품의 상세 정보를 조회합니다.")
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProduct(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakSub,
            @PathVariable("companyId") UUID companyId,
            @PathVariable("productId") UUID productId
    ) {
        ProductResponseDto response = productFacade.getProduct(
                companyId,
                productId,
                userRole,
                userHubId,
                keycloakSub
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

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

    @Operation(summary = "상품 수정", description = "업체 하위 상품 정보를 부분 수정합니다. 이름은 생성과 동일하게 trim 정책을 적용합니다.")
    @PatchMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakSub,
            @PathVariable("companyId") UUID companyId,
            @PathVariable("productId") UUID productId,
            @Valid @RequestBody ProductUpdateRequestDto request
    ) {
        productFacade.updateProduct(companyId, productId, request, userRole, userHubId, keycloakSub);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "상품 삭제", description = "업체 하위 상품을 soft delete 처리합니다.")
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakSub,
            @PathVariable("companyId") UUID companyId,
            @PathVariable("productId") UUID productId
    ) {
        productFacade.deleteProduct(companyId, productId, userRole, userHubId, keycloakSub);

        return ResponseEntity.noContent().build();
    }
}
