package com.boxoffice.companyservice.company.controller;

import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.company.dto.request.BulkHubTransferRequestDto;
import com.boxoffice.companyservice.company.dto.response.HubCompanyStockResponseDto;
import com.boxoffice.companyservice.company.dto.response.InternalCompanyHubResponseDto;
import com.boxoffice.companyservice.company.service.CompanyInternalFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Company Internal", description = "업체 내부 연동 API")
@RequestMapping("/internal/v1/companies")
public class CompanyInternalController {

    private final CompanyInternalFacade companyInternalFacade;

    @Operation(summary = "허브 소속 업체 재고 목록 조회", description = "허브 폐쇄 계획에서 사용할 hubId 기준 업체 목록과 업체별 재고 합계를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HubCompanyStockResponseDto>>> getCompaniesByHubId(
            @RequestParam("hubId") UUID hubId
    ) {
        List<HubCompanyStockResponseDto> response = companyInternalFacade.getCompaniesByHubId(hubId);
        log.info("Internal companies by hub requested. hubId={}, companyCount={}", hubId, response.size());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "업체별 허브 조회", description = "주문 생성 연동을 위해 공급 업체와 수령 업체의 소속 허브를 조회합니다.")
    @GetMapping("/hubs/{supplierId}/{receiverId}")
    public ResponseEntity<ApiResponse<InternalCompanyHubResponseDto>> getCompanyHubs(
            @PathVariable("supplierId") UUID supplierId,
            @PathVariable("receiverId") UUID receiverId
    ) {
        InternalCompanyHubResponseDto response = companyInternalFacade.getCompanyHubs(supplierId, receiverId);
        log.info("Internal company hubs requested. supplierId={}, receiverId={}", supplierId, receiverId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "업체 소속 허브 일괄 변경", description = "허브 폐쇄 재배치 완료 후 요청받은 업체들의 소속 허브를 일괄 변경합니다.")
    @PatchMapping("/bulk-hub-transfer")
    public ResponseEntity<ApiResponse<Void>> bulkTransferHub(
            @Valid @RequestBody BulkHubTransferRequestDto request
    ) {
        companyInternalFacade.bulkTransferHub(request);
        log.info("Internal company bulk hub transfer completed. companyCount={}, toHubId={}",
                request.getCompanyIds().size(), request.getToHubId());

        return ResponseEntity.ok(ApiResponse.success());
    }
}
