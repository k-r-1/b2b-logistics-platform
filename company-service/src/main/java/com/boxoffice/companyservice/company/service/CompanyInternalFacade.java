package com.boxoffice.companyservice.company.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.companyservice.company.dto.request.BulkHubTransferRequestDto;
import com.boxoffice.companyservice.company.dto.response.HubCompanyStockResponseDto;
import com.boxoffice.companyservice.company.dto.response.InternalCompanyHubResponseDto;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
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

    public InternalCompanyHubResponseDto getCompanyHubs(UUID supplierId, UUID receiverId) {
        Company supplier = companyService.getCompanyEntity(supplierId);
        Company receiver = companyService.getCompanyEntity(receiverId);
        validateCompanyHubLookupTypes(supplier, receiver);

        return new InternalCompanyHubResponseDto(supplier.getHubId(), receiver.getHubId());
    }

    private void validateCompanyHubLookupTypes(Company supplier, Company receiver) {
        if (supplier.getType() != CompanyType.SUPPLIER || receiver.getType() != CompanyType.RECEIVER) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }
}
