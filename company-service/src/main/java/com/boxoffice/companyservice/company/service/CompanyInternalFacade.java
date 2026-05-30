package com.boxoffice.companyservice.company.service;

import com.boxoffice.companyservice.company.dto.request.BulkHubTransferRequestDto;
import com.boxoffice.companyservice.company.dto.response.HubCompanyStockResponseDto;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.validator.HubValidator;
import com.boxoffice.companyservice.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyInternalFacade {

    private final CompanyService companyService;
    private final ProductService productService;
    private final HubValidator hubValidator;

    public List<HubCompanyStockResponseDto> getCompaniesByHubId(UUID hubId) {
        List<Company> companies = companyService.getCompaniesByHubId(hubId);
        List<UUID> companyIds = companies.stream()
                .map(Company::getId)
                .toList();
        Map<UUID, Long> stockCountByCompanyId = productService.getCompanyStockCountMap(companyIds);

        return companies.stream()
                .map(company -> new HubCompanyStockResponseDto(
                        company.getId(),
                        company.getName(),
                        stockCountByCompanyId.getOrDefault(company.getId(), 0L)
                ))
                .toList();
    }

    public void bulkTransferHub(BulkHubTransferRequestDto request) {
        hubValidator.validateHubActive(request.getToHubId());
        // 허브 폐쇄 스냅샷에 포함된 업체만 한 번의 UPDATE IN 쿼리로 이동한다.
        companyService.transferCompaniesHub(request.getCompanyIds(), request.getToHubId());
    }
}
