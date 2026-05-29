package com.boxoffice.companyservice.company.controller;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.exception.GlobalExceptionHandler;
import com.boxoffice.companyservice.company.dto.response.CompanyCreateResponseDto;
import com.boxoffice.companyservice.company.dto.response.CompanyResponseDto;
import com.boxoffice.companyservice.company.dto.search.CompanySearchCondition;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.boxoffice.companyservice.company.service.CompanyFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CompanyController 테스트")
class CompanyControllerTest {

    private final CompanyFacade companyFacade = mock(CompanyFacade.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new CompanyController(companyFacade))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("성공 - 업체 목록 검색 결과를 반환한다")
    void searchCompaniesReturnsPageResponse() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        CompanyResponseDto responseDto = createCompanyResponse(companyId, hubId);
        Page<CompanyResponseDto> responsePage = new PageImpl<>(List.of(responseDto));
        when(companyFacade.searchCompanies(any(), any(), eq("MASTER"))).thenReturn(responsePage);

        mockMvc.perform(get("/api/v1/companies")
                        .header("X-User-Role", "MASTER")
                        .param("name", "테스트")
                        .param("type", "SUPPLIER")
                        .param("hubId", hubId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("SUCCESS")))
                .andExpect(jsonPath("$.data.content[0].companyId", is(companyId.toString())))
                .andExpect(jsonPath("$.data.content[0].name", is("테스트 업체")))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.sort", is("createdAt,DESC")));

        ArgumentCaptor<CompanySearchCondition> conditionCaptor = ArgumentCaptor.forClass(CompanySearchCondition.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(companyFacade).searchCompanies(conditionCaptor.capture(), pageableCaptor.capture(), eq("MASTER"));
        verifyNoMoreInteractions(companyFacade);

        CompanySearchCondition condition = conditionCaptor.getValue();
        assertThat(condition.getName()).isEqualTo("테스트");
        assertThat(condition.getType()).isEqualTo("SUPPLIER");
        assertThat(condition.getHubId()).isEqualTo(hubId);

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isZero();
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(capturedPageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    @DisplayName("성공 - 업체 검색의 updatedAt 정렬 기준을 Facade로 전달한다")
    void searchCompaniesWithUpdatedAtSortPassesSortToFacade() throws Exception {
        Page<CompanyResponseDto> responsePage = Page.empty(PageRequest.of(0, 10));
        when(companyFacade.searchCompanies(any(), any(), eq("MASTER"))).thenReturn(responsePage);

        mockMvc.perform(get("/api/v1/companies")
                        .header("X-User-Role", "MASTER")
                        .param("sort", "updatedAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sort", is("updatedAt,ASC")));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(companyFacade).searchCompanies(any(), pageableCaptor.capture(), eq("MASTER"));
        verifyNoMoreInteractions(companyFacade);

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("updatedAt")).isNotNull();
        assertThat(capturedPageable.getSort().getOrderFor("updatedAt").isAscending()).isTrue();
    }

    @Test
    @DisplayName("성공 - 업체 검색 조건이 없으면 기본 페이지와 정렬로 조회한다")
    void searchCompaniesWithoutConditionUsesDefaultPageable() throws Exception {
        Page<CompanyResponseDto> responsePage = Page.empty(PageRequest.of(0, 10));
        when(companyFacade.searchCompanies(any(), any(), eq("MASTER"))).thenReturn(responsePage);

        mockMvc.perform(get("/api/v1/companies")
                        .header("X-User-Role", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page", is(0)))
                .andExpect(jsonPath("$.data.size", is(10)))
                .andExpect(jsonPath("$.data.sort", is("createdAt,DESC")));

        ArgumentCaptor<CompanySearchCondition> conditionCaptor = ArgumentCaptor.forClass(CompanySearchCondition.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(companyFacade).searchCompanies(conditionCaptor.capture(), pageableCaptor.capture(), eq("MASTER"));
        verifyNoMoreInteractions(companyFacade);

        CompanySearchCondition condition = conditionCaptor.getValue();
        assertThat(condition.getName()).isNull();
        assertThat(condition.getType()).isNull();
        assertThat(condition.getHubId()).isNull();

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isZero();
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(capturedPageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    @DisplayName("실패 - 업체 검색 type 파라미터가 잘못된 Enum 값이면 400 Bad Request를 반환한다")
    void searchCompaniesWithInvalidTypeReturnsBadRequest() throws Exception {
        when(companyFacade.searchCompanies(any(), any(), eq("MASTER")))
                .thenThrow(new BaseException(CommonErrorCode.INVALID_INPUT));

        mockMvc.perform(get("/api/v1/companies")
                        .header("X-User-Role", "MASTER")
                        .param("type", "INVALID_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is(CommonErrorCode.INVALID_INPUT.getMessage())));

        verify(companyFacade).searchCompanies(any(), any(), eq("MASTER"));
        verifyNoMoreInteractions(companyFacade);
    }

    @Test
    @DisplayName("성공 - 업체 상세 조회 요청 시 업체 상세 정보를 반환한다")
    void getCompanyReturnsCompanyResponse() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        CompanyResponseDto response = createCompanyResponse(companyId, hubId);
        when(companyFacade.getCompany(companyId, "MASTER")).thenReturn(response);

        mockMvc.perform(get("/api/v1/companies/{companyId}", companyId)
                        .header("X-User-Role", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("SUCCESS")))
                .andExpect(jsonPath("$.data.companyId", is(companyId.toString())))
                .andExpect(jsonPath("$.data.name", is("테스트 업체")))
                .andExpect(jsonPath("$.data.type", is("SUPPLIER")))
                .andExpect(jsonPath("$.data.hubId", is(hubId.toString())))
                .andExpect(jsonPath("$.data.address.address", is("경기도 고양시 덕양구 권율대로 570")));

        verify(companyFacade).getCompany(companyId, "MASTER");
        verifyNoMoreInteractions(companyFacade);
    }

    @Test
    @DisplayName("실패 - 업체 상세 조회 시 X-User-Role 헤더가 없으면 401 인증 실패를 반환한다")
    void getCompanyWithoutUserRoleHeaderReturnsUnauthorized() throws Exception {
        UUID companyId = UUID.randomUUID();
        when(companyFacade.getCompany(companyId, null))
                .thenThrow(new BaseException(CommonErrorCode.UNAUTHORIZED));

        mockMvc.perform(get("/api/v1/companies/{companyId}", companyId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is(CommonErrorCode.UNAUTHORIZED.getMessage())));

        verify(companyFacade).getCompany(companyId, null);
        verifyNoMoreInteractions(companyFacade);
    }

    @Test
    @DisplayName("성공 - POST /api/v1/companies 요청 시 201 Created와 companyId를 반환한다")
    void createCompanyReturnsCreatedAndCompanyId() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        CompanyCreateResponseDto response = createResponse(companyId, hubId);
        when(companyFacade.createCompany(any(), eq("MASTER"), isNull())).thenReturn(response);

        mockMvc.perform(post("/api/v1/companies")
                        .header("X-User-Role", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody(hubId)))
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
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        CompanyCreateResponseDto response = createResponse(companyId, hubId);
        when(companyFacade.createCompany(any(), eq("HUB_MANAGER"), eq(hubId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/companies")
                        .header("X-User-Role", "HUB_MANAGER")
                        .header("X-User-Hub-Id", hubId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody(hubId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(201)))
                .andExpect(jsonPath("$.data.companyId", is(companyId.toString())));

        verify(companyFacade).createCompany(any(), eq("HUB_MANAGER"), eq(hubId));
        verifyNoMoreInteractions(companyFacade);
    }

    @Test
    @DisplayName("실패 - X-User-Role 헤더가 없으면 401 인증 실패를 반환한다")
    void createCompanyWithoutUserRoleHeaderReturnsUnauthorized() throws Exception {
        UUID hubId = UUID.randomUUID();
        when(companyFacade.createCompany(any(), isNull(), isNull()))
                .thenThrow(new BaseException(CommonErrorCode.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody(hubId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is(CommonErrorCode.UNAUTHORIZED.getMessage())));

        verify(companyFacade).createCompany(any(), isNull(), isNull());
        verifyNoMoreInteractions(companyFacade);
    }

    @Test
    @DisplayName("실패 - 필수 요청값이 누락되면 400 검증 실패를 반환한다")
    void createCompanyWithoutNameReturnsValidationError() throws Exception {
        UUID hubId = UUID.randomUUID();
        String requestBody = """
                {
                  "type": "SUPPLIER",
                  "hubId": "%s",
                  "managerUserId": "%s",
                  "address": {
                    "zipCode": "12345",
                    "address": "경기도 고양시 덕양구 권율대로 570",
                    "detailAddress": "101호"
                  }
                }
                """.formatted(hubId, UUID.randomUUID());

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

    @Test
    @DisplayName("성공 - managerUserId가 없어도 업체 생성 요청은 Facade로 전달한다")
    void createCompanyWithoutManagerUserIdPassesRequestToFacade() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        CompanyCreateResponseDto response = createResponse(companyId, hubId);
        when(companyFacade.createCompany(any(), eq("MASTER"), isNull())).thenReturn(response);

        String requestBody = """
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

        mockMvc.perform(post("/api/v1/companies")
                        .header("X-User-Role", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(201)))
                .andExpect(jsonPath("$.data.companyId", is(companyId.toString())));

        verify(companyFacade).createCompany(any(), eq("MASTER"), isNull());
        verifyNoMoreInteractions(companyFacade);
    }

    @Test
    @DisplayName("성공 - 업체 삭제 요청 시 Facade로 삭제 요청을 전달하고 204를 반환한다")
    void deleteCompanyReturnsNoContent() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID userHubId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/v1/companies/{companyId}", companyId)
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Hub-Id", userHubId.toString())
                        .header("X-User-Id", keycloakSub))
                .andExpect(status().isNoContent());

        verify(companyFacade).deleteCompany(companyId, "MASTER", userHubId, keycloakSub);
        verifyNoMoreInteractions(companyFacade);
    }

    @Test
    @DisplayName("실패 - 삭제 요청에 X-User-Id 헤더가 없으면 401을 반환한다")
    void deleteCompanyWithoutUserIdHeaderReturnsUnauthorized() throws Exception {
        UUID companyId = UUID.randomUUID();
        doThrow(new BaseException(CommonErrorCode.UNAUTHORIZED))
                .when(companyFacade).deleteCompany(companyId, "MASTER", null, null);

        mockMvc.perform(delete("/api/v1/companies/{companyId}", companyId)
                .header("X-User-Role", "MASTER"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is(CommonErrorCode.UNAUTHORIZED.getMessage())));

        verify(companyFacade).deleteCompany(companyId, "MASTER", null, null);
        verifyNoMoreInteractions(companyFacade);
    }

    @Test
    @DisplayName("실패 - 삭제 권한이 없는 role이면 403을 반환한다")
    void deleteCompanyWithForbiddenRoleReturnsForbidden() throws Exception {
        UUID companyId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        doThrow(new BaseException(CommonErrorCode.FORBIDDEN))
                .when(companyFacade).deleteCompany(companyId, "SUPPLIER_MANAGER", null, keycloakSub);

        mockMvc.perform(delete("/api/v1/companies/{companyId}", companyId)
                        .header("X-User-Role", "SUPPLIER_MANAGER")
                .header("X-User-Id", keycloakSub))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", is(CommonErrorCode.FORBIDDEN.getMessage())));

        verify(companyFacade).deleteCompany(companyId, "SUPPLIER_MANAGER", null, keycloakSub);
        verifyNoMoreInteractions(companyFacade);
    }

    private CompanyCreateResponseDto createResponse(UUID companyId, UUID hubId) {
        return CompanyCreateResponseDto.from(createCompany(companyId, hubId));
    }

    private CompanyResponseDto createCompanyResponse(UUID companyId, UUID hubId) {
        return CompanyResponseDto.from(createCompany(companyId, hubId));
    }

    private Company createCompany(UUID companyId, UUID hubId) {
        Company company = Company.create(
                "테스트 업체",
                CompanyType.SUPPLIER,
                hubId,
                new AddressVO("12345", "경기도 고양시 덕양구 권율대로 570", "101호")
        );
        ReflectionTestUtils.setField(company, "id", companyId);
        return company;
    }

    private String createRequestBody(UUID hubId) {
        return """
                {
                  "name": "테스트 업체",
                  "type": "SUPPLIER",
                  "hubId": "%s",
                  "managerUserId": "%s",
                  "address": {
                    "zipCode": "12345",
                    "address": "경기도 고양시 덕양구 권율대로 570",
                    "detailAddress": "101호"
                  }
                }
                """.formatted(hubId, UUID.randomUUID());
    }
}
