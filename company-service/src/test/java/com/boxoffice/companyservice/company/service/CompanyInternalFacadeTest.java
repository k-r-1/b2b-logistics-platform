package com.boxoffice.companyservice.company.service;

import com.boxoffice.companyservice.company.dto.request.BulkHubTransferRequestDto;
import com.boxoffice.companyservice.company.dto.response.HubCompanyStockResponseDto;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.boxoffice.companyservice.company.validator.HubValidator;
import com.boxoffice.companyservice.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyInternalFacade 테스트")
class CompanyInternalFacadeTest {

    @InjectMocks
    private CompanyInternalFacade companyInternalFacade;

    @Mock
    private CompanyService companyService;

    @Mock
    private ProductService productService;

    @Mock
    private HubValidator hubValidator;

    @Test
    @DisplayName("성공 - 허브 소속 업체와 업체별 재고 합계를 조합해 반환한다")
    void getCompaniesByHubIdReturnsCompaniesWithStockCount() {
        // given
        UUID hubId = UUID.randomUUID();
        UUID firstCompanyId = UUID.randomUUID();
        UUID secondCompanyId = UUID.randomUUID();
        Company firstCompany = createCompany(firstCompanyId, "삼성물산", hubId);
        Company secondCompany = createCompany(secondCompanyId, "LG상사", hubId);

        when(companyService.getCompaniesByHubId(hubId)).thenReturn(List.of(firstCompany, secondCompany));
        when(productService.getCompanyStockCountMap(List.of(firstCompanyId, secondCompanyId)))
                .thenReturn(Map.of(firstCompanyId, 500L));

        // when
        List<HubCompanyStockResponseDto> response = companyInternalFacade.getCompaniesByHubId(hubId);

        // then
        verify(companyService).getCompaniesByHubId(hubId);
        verify(productService).getCompanyStockCountMap(List.of(firstCompanyId, secondCompanyId));
        verifyNoMoreInteractions(companyService, productService);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getCompanyId()).isEqualTo(firstCompanyId);
        assertThat(response.get(0).getCompanyName()).isEqualTo("삼성물산");
        assertThat(response.get(0).getStockCount()).isEqualTo(500L);
        assertThat(response.get(1).getCompanyId()).isEqualTo(secondCompanyId);
        assertThat(response.get(1).getCompanyName()).isEqualTo("LG상사");
        assertThat(response.get(1).getStockCount()).isZero();
    }

    @Test
    @DisplayName("성공 - toHubId 검증 후 요청 업체만 벌크 변경한다")
    void bulkTransferHubValidatesHubAndTransfersCompanies() {
        // given
        UUID firstCompanyId = UUID.randomUUID();
        UUID secondCompanyId = UUID.randomUUID();
        UUID toHubId = UUID.randomUUID();
        BulkHubTransferRequestDto request = new BulkHubTransferRequestDto();
        ReflectionTestUtils.setField(request, "companyIds", List.of(firstCompanyId, secondCompanyId));
        ReflectionTestUtils.setField(request, "toHubId", toHubId);

        // when
        companyInternalFacade.bulkTransferHub(request);

        // then
        verify(hubValidator).validateHubActive(toHubId);
        verify(companyService).transferCompaniesHub(List.of(firstCompanyId, secondCompanyId), toHubId);
        verifyNoMoreInteractions(hubValidator, companyService);
    }

    private Company createCompany(UUID companyId, String name, UUID hubId) {
        Company company = Company.create(name, CompanyType.SUPPLIER, hubId, null);
        ReflectionTestUtils.setField(company, "id", companyId);
        return company;
    }
}
