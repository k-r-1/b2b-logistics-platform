package com.boxoffice.companyservice.company.controller;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.exception.GlobalExceptionHandler;
import com.boxoffice.companyservice.company.dto.response.CompanyCreateResponseDto;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.boxoffice.companyservice.company.service.CompanyFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CompanyController 테스트")
class CompanyControllerTest {

    private final CompanyFacade companyFacade = mock(CompanyFacade.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new CompanyController(companyFacade))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("성공 - POST /api/v1/companies 요청 시 201 Created와 companyId를 반환한다")
    void createCompanyReturnsCreatedAndCompanyId() throws Exception {
        // given
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        CompanyCreateResponseDto response = createResponse(companyId, hubId);

        when(companyFacade.createCompany(any(), eq("MASTER"), isNull())).thenReturn(response);

        String requestBody = createRequestBody(hubId);

        // when & then
        mockMvc.perform(post("/api/v1/companies")
                        .header("X-User-Role", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(201)))
                .andExpect(jsonPath("$.message", is("SUCCESS")))
                .andExpect(jsonPath("$.data.companyId", is(companyId.toString())));

        verify(companyFacade).createCompany(any(), eq("MASTER"), isNull());
        verifyNoMoreInteractions(companyFacade);
    }

    @Test
    @DisplayName("성공 - X-User-Hub-Id 헤더를 Facade로 전달한다")
    void createCompanyPassesUserHubIdHeader() throws Exception {
        // given
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        CompanyCreateResponseDto response = createResponse(companyId, hubId);

        when(companyFacade.createCompany(any(), eq("HUB_MANAGER"), eq(hubId))).thenReturn(response);

        String requestBody = createRequestBody(hubId);

        // when & then
        mockMvc.perform(post("/api/v1/companies")
                        .header("X-User-Role", "HUB_MANAGER")
                        .header("X-User-Hub-Id", hubId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(201)))
                .andExpect(jsonPath("$.data.companyId", is(companyId.toString())));

        verify(companyFacade).createCompany(any(), eq("HUB_MANAGER"), eq(hubId));
        verifyNoMoreInteractions(companyFacade);
    }

    @Test
    @DisplayName("실패 - X-User-Role 헤더가 없으면 401 인증 실패를 반환한다")
    void createCompanyWithoutUserRoleHeaderReturnsUnauthorized() throws Exception {
        // given
        UUID hubId = UUID.randomUUID();
        // X-User-Role은 required=false로 받고, Facade에서 공통 인증 실패 예외로 변환한다.
        when(companyFacade.createCompany(any(), isNull(), isNull()))
                .thenThrow(new BaseException(CommonErrorCode.UNAUTHORIZED));

        String requestBody = createRequestBody(hubId);

        // when & then
        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is(CommonErrorCode.UNAUTHORIZED.getMessage())));

        verify(companyFacade).createCompany(any(), isNull(), isNull());
        verifyNoMoreInteractions(companyFacade);
    }

    @Test
    @DisplayName("실패 - 필수 요청값이 누락되면 400 검증 실패를 반환한다")
    void createCompanyWithoutNameReturnsValidationError() throws Exception {
        // given
        UUID hubId = UUID.randomUUID();
        String requestBody = """
                {
                  "type": "SUPPLIER",
                  "hubId": "%s",
                  "address": {
                    "zipCode": "12345",
                    "address": "경기도 고양시 덕양구 권율대로 570",
                    "detailAddress": "101호"
                  }
                }
                """.formatted(hubId);

        // when & then
        mockMvc.perform(post("/api/v1/companies")
                        .header("X-User-Role", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors", hasItem("name: 업체명은 필수입니다.")));

        verifyNoInteractions(companyFacade);
    }

    private CompanyCreateResponseDto createResponse(UUID companyId, UUID hubId) {
        Company company = Company.create(
                "테스트 업체",
                CompanyType.SUPPLIER,
                hubId,
                new AddressVO("12345", "경기도 고양시 덕양구 권율대로 570", "101호")
        );
        // BaseEntity id는 JPA 저장 시 생성되므로 컨트롤러 테스트에서는 응답용 id만 주입한다.
        ReflectionTestUtils.setField(company, "id", companyId);
        return CompanyCreateResponseDto.from(company);
    }

    private String createRequestBody(UUID hubId) {
        return """
                {
                  "name": "테스트 업체",
                  "type": "SUPPLIER",
                  "hubId": "%s",
                  "address": {
                    "zipCode": "12345",
                    "address": "경기도 고양시 덕양구 권율대로 570",
                    "detailAddress": "101호"
                  }
                }
                """.formatted(hubId);
    }
}
