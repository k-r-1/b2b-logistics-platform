package com.boxoffice.companyservice.product.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.company.client.UserClient;
import com.boxoffice.companyservice.company.client.dto.UserResponseDto;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.boxoffice.companyservice.company.exception.CompanyErrorCode;
import com.boxoffice.companyservice.company.service.CompanyService;
import com.boxoffice.companyservice.company.validator.HubValidator;
import com.boxoffice.companyservice.product.dto.request.ProductCreateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductFacade 테스트")
class ProductFacadeTest {

    @InjectMocks
    private ProductFacade productFacade;

    @Mock
    private UserClient userClient;

    @Mock
    private CompanyService companyService;

    @Mock
    private ProductService productService;

    @Mock
    private HubValidator hubValidator;

    @Test
    @DisplayName("성공 - MASTER는 상품을 생성할 수 있다")
    void createProductWithMasterRole() {
        // given
        UUID companyId = UUID.randomUUID();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductCreateRequestDto request = createRequest();
        ProductCreateResponseDto expectedResponse = mock(ProductCreateResponseDto.class);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(productService.createProduct(companyId, request)).thenReturn(expectedResponse);

        // when
        ProductCreateResponseDto response = productFacade.createProduct(companyId, request, "MASTER", null, null);

        // then
        assertThat(response).isSameAs(expectedResponse);
        verify(companyService).getCompanyEntity(companyId);
        verify(hubValidator).validateHubActive(company.getHubId());
        verify(productService).createProduct(companyId, request);
        verifyNoMoreInteractions(companyService, productService, hubValidator);
        verifyNoInteractions(userClient);
    }

    @Test
    @DisplayName("성공 - HUB_MANAGER는 담당 허브 업체의 상품을 생성할 수 있다")
    void createProductWithHubManagerRoleAndSameHub() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Company company = createCompany(companyId, hubId);
        ProductCreateRequestDto request = createRequest();
        ProductCreateResponseDto expectedResponse = mock(ProductCreateResponseDto.class);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(productService.createProduct(companyId, request)).thenReturn(expectedResponse);

        // when
        ProductCreateResponseDto response = productFacade.createProduct(companyId, request, "HUB_MANAGER", hubId, null);

        // then
        assertThat(response).isSameAs(expectedResponse);
        verify(companyService).getCompanyEntity(companyId);
        verify(hubValidator).validateHubActive(company.getHubId());
        verify(productService).createProduct(companyId, request);
        verifyNoMoreInteractions(companyService, productService, hubValidator);
        verifyNoInteractions(userClient);
    }

    @Test
    @DisplayName("성공 - SUPPLIER_MANAGER는 본인 업체의 상품을 생성할 수 있다")
    void createProductWithSupplierManagerRoleAndOwnCompany() {
        // given
        UUID companyId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductCreateRequestDto request = createRequest();
        ProductCreateResponseDto expectedResponse = mock(ProductCreateResponseDto.class);
        ApiResponse<UserResponseDto> userResponse = createUserResponse(companyId);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(userClient.getUserByKeycloakSub(keycloakSub)).thenReturn(userResponse);
        when(productService.createProduct(companyId, request)).thenReturn(expectedResponse);

        // when
        ProductCreateResponseDto response = productFacade.createProduct(
                companyId,
                request,
                "SUPPLIER_MANAGER",
                null,
                keycloakSub
        );

        // then
        assertThat(response).isSameAs(expectedResponse);
        verify(companyService).getCompanyEntity(companyId);
        verify(userClient).getUserByKeycloakSub(keycloakSub);
        verify(hubValidator).validateHubActive(company.getHubId());
        verify(productService).createProduct(companyId, request);
        verifyNoMoreInteractions(userClient, companyService, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - 생성 요청이 없으면 입력값 오류로 처리한다")
    void createProductWithoutRequest() {
        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(UUID.randomUUID(), null, "MASTER", null, null)
        );

        // then
        assertInvalidInput(throwable);
        verifyNoInteractions(userClient, companyService, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - role 헤더가 없으면 인증 실패로 처리한다")
    void createProductWithoutRole() {
        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(UUID.randomUUID(), createRequest(), null, null, null)
        );

        // then
        assertUnauthorized(throwable);
        verifyNoInteractions(userClient, companyService, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - role 헤더가 공백이면 인증 실패로 처리한다")
    void createProductWithBlankRole() {
        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(UUID.randomUUID(), createRequest(), "   ", null, null)
        );

        // then
        assertUnauthorized(throwable);
        verifyNoInteractions(userClient, companyService, productService, hubValidator);
    }

    @ParameterizedTest(name = "실패 - role={0}은 상품을 생성할 수 없다")
    @ValueSource(strings = {"DELIVERY_MANAGER", "UNKNOWN_ROLE"})
    void createProductWithForbiddenRole(String userRole) {
        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(UUID.randomUUID(), createRequest(), userRole, null, null)
        );

        // then
        assertForbidden(throwable);
        verifyNoInteractions(userClient, companyService, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - HUB_MANAGER는 담당 허브가 아닌 업체의 상품을 생성할 수 없다")
    void createProductWithHubManagerRoleAndDifferentHub() {
        // given
        UUID companyId = UUID.randomUUID();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductCreateRequestDto request = createRequest();

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(companyId, request, "HUB_MANAGER", UUID.randomUUID(), null)
        );

        // then
        assertForbidden(throwable);
        verify(companyService).getCompanyEntity(companyId);
        verifyNoMoreInteractions(companyService);
        verifyNoInteractions(userClient, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - SUPPLIER_MANAGER 요청에서 X-User-Id가 없으면 인증 실패로 처리한다")
    void createProductWithSupplierManagerRoleWithoutKeycloakSub() {
        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(UUID.randomUUID(), createRequest(), "SUPPLIER_MANAGER", null, null)
        );

        // then
        assertUnauthorized(throwable);
        verifyNoInteractions(userClient, companyService, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - user-service 응답 data가 없으면 상품을 생성할 수 없다")
    void createProductWithSupplierManagerRoleWithoutUserDataInUserResponse() {
        // given
        UUID companyId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductCreateRequestDto request = createRequest();
        ApiResponse<UserResponseDto> userResponse = ApiResponse.success(null);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(userClient.getUserByKeycloakSub(keycloakSub)).thenReturn(userResponse);

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(companyId, request, "SUPPLIER_MANAGER", null, keycloakSub)
        );

        // then
        assertForbidden(throwable);
        verify(companyService).getCompanyEntity(companyId);
        verify(userClient).getUserByKeycloakSub(keycloakSub);
        verifyNoMoreInteractions(userClient, companyService);
        verifyNoInteractions(productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - user-service 응답 data에 companyId가 없으면 상품을 생성할 수 없다")
    void createProductWithSupplierManagerRoleWithoutCompanyIdInUserResponse() {
        // given
        UUID companyId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductCreateRequestDto request = createRequest();
        ApiResponse<UserResponseDto> userResponse = createUserResponse(null);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(userClient.getUserByKeycloakSub(keycloakSub)).thenReturn(userResponse);

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(companyId, request, "SUPPLIER_MANAGER", null, keycloakSub)
        );

        // then
        assertForbidden(throwable);
        verify(companyService).getCompanyEntity(companyId);
        verify(userClient).getUserByKeycloakSub(keycloakSub);
        verifyNoMoreInteractions(userClient, companyService);
        verifyNoInteractions(productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - SUPPLIER_MANAGER는 다른 업체의 상품을 생성할 수 없다")
    void createProductWithSupplierManagerRoleAndDifferentCompany() {
        // given
        UUID companyId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductCreateRequestDto request = createRequest();
        ApiResponse<UserResponseDto> userResponse = createUserResponse(UUID.randomUUID());

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(userClient.getUserByKeycloakSub(keycloakSub)).thenReturn(userResponse);

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(companyId, request, "SUPPLIER_MANAGER", null, keycloakSub)
        );

        // then
        assertForbidden(throwable);
        verify(companyService).getCompanyEntity(companyId);
        verify(userClient).getUserByKeycloakSub(keycloakSub);
        verifyNoMoreInteractions(userClient, companyService);
        verifyNoInteractions(productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - user-service 호출이 실패하면 Feign 실패 예외로 변환한다")
    void createProductWhenUserServiceCallFails() {
        // given
        UUID companyId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductCreateRequestDto request = createRequest();

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(userClient.getUserByKeycloakSub(keycloakSub)).thenThrow(createFeignException(500));

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(companyId, request, "SUPPLIER_MANAGER", null, keycloakSub)
        );

        // then
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FEIGN_CLIENT_ERROR));
        verify(companyService).getCompanyEntity(companyId);
        verify(userClient).getUserByKeycloakSub(keycloakSub);
        verifyNoMoreInteractions(userClient, companyService);
        verifyNoInteractions(productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - 상품 관리 허브가 비활성화되어 있으면 상품을 생성할 수 없다")
    void createProductWithInactiveHub() {
        // given
        UUID companyId = UUID.randomUUID();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductCreateRequestDto request = createRequest();
        BaseException inactiveHubException = new BaseException(CompanyErrorCode.HUB_INACTIVE);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        doThrow(inactiveHubException).when(hubValidator).validateHubActive(company.getHubId());

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(companyId, request, "MASTER", null, null)
        );

        // then
        assertThat(throwable).isSameAs(inactiveHubException);
        verify(companyService).getCompanyEntity(companyId);
        verify(hubValidator).validateHubActive(company.getHubId());
        verifyNoMoreInteractions(companyService, hubValidator);
        verifyNoInteractions(userClient, productService);
    }

    @Test
    @DisplayName("실패 - 존재하지 않거나 삭제된 업체에는 상품을 생성할 수 없다")
    void createProductWithUnknownCompanyIdThrowsNotFound() {
        // given
        UUID companyId = UUID.randomUUID();
        BaseException notFoundException = new BaseException(CompanyErrorCode.COMPANY_NOT_FOUND);
        when(companyService.getCompanyEntity(companyId)).thenThrow(notFoundException);

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.createProduct(companyId, createRequest(), "MASTER", null, null)
        );

        // then
        assertThat(throwable).isSameAs(notFoundException);
        verify(companyService).getCompanyEntity(companyId);
        verifyNoMoreInteractions(companyService);
        verifyNoInteractions(userClient, productService, hubValidator);
    }

    private void assertInvalidInput(Throwable throwable) {
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    private void assertUnauthorized(Throwable throwable) {
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED));
    }

    private void assertForbidden(Throwable throwable) {
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    private Company createCompany(UUID companyId, UUID hubId) {
        Company company = Company.create(
                "테스트 업체",
                CompanyType.SUPPLIER,
                hubId,
                null
        );
        ReflectionTestUtils.setField(company, "id", companyId);
        return company;
    }

    private ProductCreateRequestDto createRequest() {
        ProductCreateRequestDto request = new ProductCreateRequestDto();
        ReflectionTestUtils.setField(request, "name", "테스트 상품");
        ReflectionTestUtils.setField(request, "price", 10000);
        ReflectionTestUtils.setField(request, "stockQuantity", 50);
        return request;
    }

    private ApiResponse<UserResponseDto> createUserResponse(UUID companyId) {
        UserResponseDto user = new UserResponseDto();
        ReflectionTestUtils.setField(user, "companyId", companyId);

        return ApiResponse.success(user);
    }

    private FeignException createFeignException(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/v1/users/keycloak/{keycloakSub}",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        return FeignException.errorStatus(
                "UserClient#getUserByKeycloakSub(String)",
                feign.Response.builder()
                        .status(status)
                        .reason("Feign error")
                        .request(request)
                        .build()
        );
    }
}

