package com.boxoffice.companyservice.company.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.common.util.PageableUtils;
import com.boxoffice.companyservice.company.dto.request.CompanyCreateRequestDto;
import com.boxoffice.companyservice.company.dto.request.CompanyUpdateRequestDto;
import com.boxoffice.companyservice.company.dto.response.CompanyCreateResponseDto;
import com.boxoffice.companyservice.company.dto.response.CompanyResponseDto;
import com.boxoffice.companyservice.company.dto.search.CompanySearchCondition;
import com.boxoffice.companyservice.company.service.CompanyFacade;
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
@RequestMapping("/api/v1/companies")
@Tag(name = "Company", description = "업체 API")
public class CompanyController {

    private final CompanyFacade companyFacade;

    @Operation(summary = "업체 목록 및 검색 조회", description = "조건에 따라 업체 목록을 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CompanyResponseDto>>> searchCompanies(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @ModelAttribute CompanySearchCondition condition,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // PageableUtils를 사용하여 페이지 사이즈(10, 30, 50)를 강제로 보정한다.
        Pageable validPageable = PageableUtils.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().stream().findFirst().map(Sort.Order::getProperty).orElse("createdAt"),
                pageable.getSort().stream().findFirst().map(Sort.Order::isDescending).orElse(true)
        );

        Page<CompanyResponseDto> companies = companyFacade.searchCompanies(condition, validPageable, userRole);
        String sort = validPageable.getSort().stream()
                .findFirst()
                .map(order -> order.getProperty() + "," + order.getDirection())
                .orElse("createdAt,DESC");

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(companies, sort)));
    }

    @Operation(summary = "업체 상세 조회", description = "업체 ID(UUID)를 통해 특정 업체의 상세 정보를 조회합니다.")
    @GetMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponseDto>> getCompany(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable("companyId") UUID companyId
    ) {
        CompanyResponseDto response = companyFacade.getCompany(companyId, userRole);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "업체 생성", description = "MASTER 또는 담당 허브 HUB_MANAGER가 업체를 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CompanyCreateResponseDto>> createCompany(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId,
            @Valid @RequestBody CompanyCreateRequestDto request
    ) {
        CompanyCreateResponseDto response = companyFacade.createCompany(request, userRole, userHubId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response));
    }

    @Operation(summary = "업체 수정", description = "업체명, 타입, 주소를 수정합니다.")
    @PatchMapping("/{companyId}")
    public ResponseEntity<Void> updateCompany(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakSub,
            @PathVariable("companyId") UUID companyId,
            @Valid @RequestBody CompanyUpdateRequestDto request
    ) {
        companyFacade.updateCompany(companyId, request, userRole, userHubId, keycloakSub);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "업체 삭제", description = "MASTER 또는 담당 허브 HUB_MANAGER가 업체를 삭제합니다.")
    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakSub,
            @PathVariable("companyId") UUID companyId
    ) {
        companyFacade.deleteCompany(companyId, userRole, userHubId, keycloakSub);

        return ResponseEntity.noContent().build();
    }
}
