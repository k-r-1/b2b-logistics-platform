package com.boxoffice.companyservice.company.controller;

import com.boxoffice.common.exception.GlobalExceptionHandler;
import com.boxoffice.companyservice.company.dto.response.HubCompanyStockResponseDto;
import com.boxoffice.companyservice.company.dto.response.InternalCompanyHubResponseDto;
import com.boxoffice.companyservice.company.service.CompanyInternalFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CompanyInternalController 테스트")
class CompanyInternalControllerTest {

    private final CompanyInternalFacade companyInternalFacade = mock(CompanyInternalFacade.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new CompanyInternalController(companyInternalFacade))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("성공 - 허브 소속 업체 목록과 업체별 재고 합계를 반환한다")
    void getCompaniesByHubIdReturnsCompaniesWithStockCount() throws Exception {
        // given
        UUID hubId = UUID.randomUUID();
        UUID firstCompanyId = UUID.randomUUID();
        UUID secondCompanyId = UUID.randomUUID();

        when(companyInternalFacade.getCompaniesByHubId(hubId)).thenReturn(List.of(
                new HubCompanyStockResponseDto(firstCompanyId, "삼성물산", 500L),
                new HubCompanyStockResponseDto(secondCompanyId, "LG상사", 0L)
        ));

        // when & then
        mockMvc.perform(get("/internal/v1/companies").param("hubId", hubId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("SUCCESS")))
                .andExpect(jsonPath("$.data[0].companyId", is(firstCompanyId.toString())))
                .andExpect(jsonPath("$.data[0].companyName", is("삼성물산")))
                .andExpect(jsonPath("$.data[0].stockCount", is(500)))
                .andExpect(jsonPath("$.data[1].companyId", is(secondCompanyId.toString())))
                .andExpect(jsonPath("$.data[1].companyName", is("LG상사")))
                .andExpect(jsonPath("$.data[1].stockCount", is(0)));

        verify(companyInternalFacade).getCompaniesByHubId(hubId);
        verifyNoMoreInteractions(companyInternalFacade);
    }

    @Test
    @DisplayName("성공 - 공급업체와 수령업체의 허브 ID를 반환한다")
    void getCompanyHubsReturnsSupplierAndReceiverHubIds() throws Exception {
        UUID supplierId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID supplierHubId = UUID.randomUUID();
        UUID receiverHubId = UUID.randomUUID();

        when(companyInternalFacade.getCompanyHubs(supplierId, receiverId))
                .thenReturn(new InternalCompanyHubResponseDto(supplierHubId, receiverHubId));

        mockMvc.perform(get("/internal/v1/companies/hubs/{supplierId}/{receiverId}", supplierId, receiverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("SUCCESS")))
                .andExpect(jsonPath("$.data.supplierHubId", is(supplierHubId.toString())))
                .andExpect(jsonPath("$.data.receiverHubId", is(receiverHubId.toString())));

        verify(companyInternalFacade).getCompanyHubs(supplierId, receiverId);
        verifyNoMoreInteractions(companyInternalFacade);
    }

    @Test
    @DisplayName("성공 - 요청받은 업체들의 소속 허브를 일괄 변경한다")
    void bulkTransferHubReturnsSuccess() throws Exception {
        // given
        UUID firstCompanyId = UUID.randomUUID();
        UUID secondCompanyId = UUID.randomUUID();
        UUID toHubId = UUID.randomUUID();

        // when & then
        mockMvc.perform(patch("/internal/v1/companies/bulk-hub-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyIds": ["%s", "%s"],
                                  "toHubId": "%s"
                                }
                                """.formatted(firstCompanyId, secondCompanyId, toHubId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("SUCCESS")));

        verify(companyInternalFacade).bulkTransferHub(argThat(request ->
                request.getCompanyIds().equals(List.of(firstCompanyId, secondCompanyId))
                        && request.getToHubId().equals(toHubId)
        ));
        verifyNoMoreInteractions(companyInternalFacade);
    }
}
