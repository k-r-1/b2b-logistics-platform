package com.boxoffice.companyservice.product.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.common.util.PageableUtils;
import com.boxoffice.companyservice.product.dto.response.ProductResponseDto;
import com.boxoffice.companyservice.product.dto.search.ProductSearchCondition;
import com.boxoffice.companyservice.product.service.ProductFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@Tag(name = "Product", description = "상품 API")
public class ProductSearchController {

    private final ProductFacade productFacade;

    @Operation(summary = "전체 상품 검색", description = "전체 상품 목록을 조건에 따라 페이지 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDto>>> searchProducts(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakSub,
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
}
