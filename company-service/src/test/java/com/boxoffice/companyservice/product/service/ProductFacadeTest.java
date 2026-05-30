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
import com.boxoffice.companyservice.product.dto.request.ProductUpdateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import com.boxoffice.companyservice.product.dto.response.ProductResponseDto;
import com.boxoffice.companyservice.product.dto.search.ProductSearchCondition;
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
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
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

    @Mock
    private AuditorAware<UUID> auditorAware;

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
    @DisplayName("성공 - MASTER는 상품을 수정할 수 있다")
    void updateProductWithMasterRole() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductUpdateRequestDto request = createUpdateRequest();

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);

        // when
        productFacade.updateProduct(companyId, productId, request, "MASTER", null, null);

        // then
        verify(companyService).getCompanyEntity(companyId);
        verify(hubValidator).validateHubActive(company.getHubId());
        verify(productService).updateProduct(companyId, productId, request);
        verifyNoMoreInteractions(companyService, productService, hubValidator);
        verifyNoInteractions(userClient);
    }

    @Test
    @DisplayName("성공 - HUB_MANAGER는 담당 허브 업체 상품을 수정할 수 있다")
    void updateProductWithHubManagerRoleAndSameHub() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Company company = createCompany(companyId, hubId);
        ProductUpdateRequestDto request = createUpdateRequest();

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);

        // when
        productFacade.updateProduct(companyId, productId, request, "HUB_MANAGER", hubId, null);

        // then
        verify(companyService).getCompanyEntity(companyId);
        verify(hubValidator).validateHubActive(company.getHubId());
        verify(productService).updateProduct(companyId, productId, request);
        verifyNoMoreInteractions(companyService, productService, hubValidator);
        verifyNoInteractions(userClient);
    }

    @Test
    @DisplayName("성공 - SUPPLIER_MANAGER는 본인 업체 상품을 수정할 수 있다")
    void updateProductWithSupplierManagerRoleAndOwnCompany() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductUpdateRequestDto request = createUpdateRequest();

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(userClient.getUserByKeycloakSub(keycloakSub)).thenReturn(createUserResponse(companyId));

        // when
        productFacade.updateProduct(companyId, productId, request, "SUPPLIER_MANAGER", null, keycloakSub);

        // then
        verify(companyService).getCompanyEntity(companyId);
        verify(userClient).getUserByKeycloakSub(keycloakSub);
        verify(hubValidator).validateHubActive(company.getHubId());
        verify(productService).updateProduct(companyId, productId, request);
        verifyNoMoreInteractions(userClient, companyService, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - 빈 PATCH 요청은 입력값 오류로 처리한다")
    void updateProductWithEmptyRequest() {
        // given
        ProductUpdateRequestDto request = new ProductUpdateRequestDto();

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.updateProduct(UUID.randomUUID(), UUID.randomUUID(), request, "MASTER", null, null)
        );

        // then
        assertInvalidInput(throwable);
        verifyNoInteractions(userClient, companyService, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - HUB_MANAGER는 다른 허브 업체 상품을 수정할 수 없다")
    void updateProductWithHubManagerRoleAndDifferentHub() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Company company = createCompany(companyId, UUID.randomUUID());

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.updateProduct(companyId, productId, createUpdateRequest(), "HUB_MANAGER", UUID.randomUUID(), null)
        );

        // then
        assertForbidden(throwable);
        verify(companyService).getCompanyEntity(companyId);
        verifyNoMoreInteractions(companyService);
        verifyNoInteractions(userClient, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - SUPPLIER_MANAGER는 다른 업체 상품을 수정할 수 없다")
    void updateProductWithSupplierManagerRoleAndDifferentCompany() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        Company company = createCompany(companyId, UUID.randomUUID());

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(userClient.getUserByKeycloakSub(keycloakSub)).thenReturn(createUserResponse(UUID.randomUUID()));

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.updateProduct(companyId, productId, createUpdateRequest(), "SUPPLIER_MANAGER", null, keycloakSub)
        );

        // then
        assertForbidden(throwable);
        verify(companyService).getCompanyEntity(companyId);
        verify(userClient).getUserByKeycloakSub(keycloakSub);
        verifyNoMoreInteractions(userClient, companyService);
        verifyNoInteractions(productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - DELIVERY_MANAGER는 상품을 수정할 수 없다")
    void updateProductWithDeliveryManagerRole() {
        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.updateProduct(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        createUpdateRequest(),
                        "DELIVERY_MANAGER",
                        null,
                        null
                )
        );

        // then
        assertForbidden(throwable);
        verifyNoInteractions(userClient, companyService, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - 알 수 없는 role은 상품을 수정할 수 없다")
    void updateProductWithUnknownRole() {
        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.updateProduct(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        createUpdateRequest(),
                        "UNKNOWN_ROLE",
                        null,
                        null
                )
        );

        // then
        assertForbidden(throwable);
        verifyNoInteractions(userClient, companyService, productService, hubValidator);
    }

    @Test
    @DisplayName("성공 - MASTER는 상품을 삭제할 수 있다")
    void deleteProductWithMasterRole() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        UUID auditorId = UUID.fromString(keycloakSub);
        Company company = createCompany(companyId, UUID.randomUUID());

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(auditorId));

        // when
        productFacade.deleteProduct(companyId, productId, "MASTER", null, keycloakSub);

        // then
        verify(companyService).getCompanyEntity(companyId);
        verify(auditorAware).getCurrentAuditor();
        verify(productService).deleteProduct(companyId, productId, auditorId);
        verifyNoMoreInteractions(companyService, productService, auditorAware);
        verifyNoInteractions(userClient);
        verifyNoInteractions(hubValidator);
    }

    @Test
    @DisplayName("성공 - HUB_MANAGER는 담당 허브 업체 상품을 삭제할 수 있다")
    void deleteProductWithHubManagerRoleAndSameHub() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        UUID auditorId = UUID.fromString(keycloakSub);
        Company company = createCompany(companyId, hubId);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(auditorId));

        // when
        productFacade.deleteProduct(companyId, productId, "HUB_MANAGER", hubId, keycloakSub);

        // then
        verify(companyService).getCompanyEntity(companyId);
        verify(auditorAware).getCurrentAuditor();
        verify(productService).deleteProduct(companyId, productId, auditorId);
        verifyNoMoreInteractions(companyService, productService, auditorAware);
        verifyNoInteractions(userClient);
        verifyNoInteractions(hubValidator);
    }

    @Test
    @DisplayName("실패 - HUB_MANAGER는 다른 허브 업체 상품을 삭제할 수 없다")
    void deleteProductWithHubManagerRoleAndDifferentHub() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        Company company = createCompany(companyId, UUID.randomUUID());

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.deleteProduct(companyId, productId, "HUB_MANAGER", UUID.randomUUID(), keycloakSub)
        );

        // then
        assertForbidden(throwable);
        verify(companyService).getCompanyEntity(companyId);
        verifyNoMoreInteractions(companyService);
        verifyNoInteractions(userClient, productService, hubValidator);
    }

    @ParameterizedTest(name = "실패 - role={0}은 상품을 삭제할 수 없다")
    @ValueSource(strings = {"DELIVERY_MANAGER", "SUPPLIER_MANAGER", "UNKNOWN_ROLE"})
    void deleteProductWithForbiddenRole(String userRole) {
        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.deleteProduct(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        userRole,
                        null,
                        UUID.randomUUID().toString()
                )
        );

        // then
        assertForbidden(throwable);
        verifyNoInteractions(userClient, companyService, productService, hubValidator);
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

    @Test
    @DisplayName("성공 - MASTER는 상품 상세를 조회할 수 있다")
    void getProductWithMasterRole() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductResponseDto expectedResponse = mock(ProductResponseDto.class);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(productService.getProduct(companyId, productId)).thenReturn(expectedResponse);

        // when
        ProductResponseDto response = productFacade.getProduct(companyId, productId, "MASTER", null, null);

        // then
        assertThat(response).isSameAs(expectedResponse);
        verify(companyService).getCompanyEntity(companyId);
        verify(productService).getProduct(companyId, productId);
        verifyNoMoreInteractions(companyService, productService);
        verifyNoInteractions(userClient, hubValidator);
    }

    @Test
    @DisplayName("성공 - SUPPLIER_MANAGER는 본인 업체 상품을 조회할 수 있다")
    void getProductWithSupplierManagerRoleAndOwnCompany() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        Company company = createCompany(companyId, UUID.randomUUID());
        ProductResponseDto expectedResponse = mock(ProductResponseDto.class);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(userClient.getUserByKeycloakSub(keycloakSub)).thenReturn(createUserResponse(companyId));
        when(productService.getProduct(companyId, productId)).thenReturn(expectedResponse);

        // when
        ProductResponseDto response = productFacade.getProduct(
                companyId,
                productId,
                "SUPPLIER_MANAGER",
                null,
                keycloakSub
        );

        // then
        assertThat(response).isSameAs(expectedResponse);
        verify(companyService).getCompanyEntity(companyId);
        verify(userClient).getUserByKeycloakSub(keycloakSub);
        verify(productService).getProduct(companyId, productId);
        verifyNoMoreInteractions(userClient, companyService, productService);
        verifyNoInteractions(hubValidator);
    }

    @Test
    @DisplayName("실패 - HUB_MANAGER는 담당 허브가 아닌 업체의 상품을 조회할 수 없다")
    void getProductWithHubManagerRoleAndDifferentHub() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Company company = createCompany(companyId, UUID.randomUUID());

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.getProduct(companyId, productId, "HUB_MANAGER", UUID.randomUUID(), null)
        );

        // then
        assertForbidden(throwable);
        verify(companyService).getCompanyEntity(companyId);
        verifyNoMoreInteractions(companyService);
        verifyNoInteractions(userClient, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - SUPPLIER_MANAGER는 다른 업체 상품을 조회할 수 없다")
    void getProductWithSupplierManagerRoleAndDifferentCompany() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        Company company = createCompany(companyId, UUID.randomUUID());

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(userClient.getUserByKeycloakSub(keycloakSub)).thenReturn(createUserResponse(UUID.randomUUID()));

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.getProduct(companyId, productId, "SUPPLIER_MANAGER", null, keycloakSub)
        );

        // then
        assertForbidden(throwable);
        verify(companyService).getCompanyEntity(companyId);
        verify(userClient).getUserByKeycloakSub(keycloakSub);
        verifyNoMoreInteractions(userClient, companyService);
        verifyNoInteractions(productService, hubValidator);
    }

    @Test
    @DisplayName("성공 - 상품 목록을 검색할 수 있다")
    void searchProductsWithReadableRole() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Company company = createCompany(companyId, hubId);
        ProductSearchCondition condition = new ProductSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductResponseDto> expectedResponse = Page.empty(pageable);

        when(companyService.getCompanyEntity(companyId)).thenReturn(company);
        when(productService.searchProducts(companyId, condition, pageable)).thenReturn(expectedResponse);

        // when
        Page<ProductResponseDto> response = productFacade.searchProducts(
                companyId,
                condition,
                pageable,
                "HUB_MANAGER",
                hubId,
                null
        );

        // then
        assertThat(response).isSameAs(expectedResponse);
        verify(companyService).getCompanyEntity(companyId);
        verify(productService).searchProducts(companyId, condition, pageable);
        verifyNoMoreInteractions(companyService, productService);
        verifyNoInteractions(userClient, hubValidator);
    }

    @Test
    @DisplayName("성공 - HUB_MANAGER 전체 상품 검색은 담당 허브 조건으로 제한된다")
    void searchAllProductsWithHubManagerRoleAppliesHubScope() {
        // given
        UUID hubId = UUID.randomUUID();
        ProductSearchCondition condition = new ProductSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductResponseDto> expectedResponse = Page.empty(pageable);

        when(productService.searchProducts(condition, pageable)).thenReturn(expectedResponse);

        // when
        Page<ProductResponseDto> response = productFacade.searchProducts(
                condition,
                pageable,
                "HUB_MANAGER",
                hubId,
                null
        );

        // then
        assertThat(response).isSameAs(expectedResponse);
        assertThat(condition.getHubId()).isEqualTo(hubId);
        verify(productService).searchProducts(condition, pageable);
        verifyNoMoreInteractions(productService);
        verifyNoInteractions(userClient, companyService, hubValidator);
    }

    @Test
    @DisplayName("성공 - SUPPLIER_MANAGER 전체 상품 검색은 본인 업체 조건으로 제한된다")
    void searchAllProductsWithSupplierManagerRoleAppliesCompanyScope() {
        // given
        UUID companyId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        ProductSearchCondition condition = new ProductSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductResponseDto> expectedResponse = Page.empty(pageable);

        when(userClient.getUserByKeycloakSub(keycloakSub)).thenReturn(createUserResponse(companyId));
        when(productService.searchProducts(condition, pageable)).thenReturn(expectedResponse);

        // when
        Page<ProductResponseDto> response = productFacade.searchProducts(
                condition,
                pageable,
                "SUPPLIER_MANAGER",
                null,
                keycloakSub
        );

        // then
        assertThat(response).isSameAs(expectedResponse);
        assertThat(condition.getCompanyId()).isEqualTo(companyId);
        verify(userClient).getUserByKeycloakSub(keycloakSub);
        verify(productService).searchProducts(condition, pageable);
        verifyNoMoreInteractions(userClient, productService);
        verifyNoInteractions(companyService, hubValidator);
    }

    @Test
    @DisplayName("실패 - HUB_MANAGER는 다른 허브 조건으로 전체 상품을 검색할 수 없다")
    void searchAllProductsWithHubManagerRoleAndDifferentHubCondition() {
        // given
        ProductSearchCondition condition = ProductSearchCondition.builder()
                .hubId(UUID.randomUUID())
                .build();

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.searchProducts(condition, PageRequest.of(0, 10), "HUB_MANAGER", UUID.randomUUID(), null)
        );

        // then
        assertForbidden(throwable);
        verifyNoInteractions(userClient, companyService, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - SUPPLIER_MANAGER는 다른 업체 조건으로 전체 상품을 검색할 수 없다")
    void searchAllProductsWithSupplierManagerRoleAndDifferentCompanyCondition() {
        // given
        UUID companyId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        ProductSearchCondition condition = ProductSearchCondition.builder()
                .companyId(UUID.randomUUID())
                .build();

        when(userClient.getUserByKeycloakSub(keycloakSub)).thenReturn(createUserResponse(companyId));

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.searchProducts(condition, PageRequest.of(0, 10), "SUPPLIER_MANAGER", null, keycloakSub)
        );

        // then
        assertForbidden(throwable);
        verify(userClient).getUserByKeycloakSub(keycloakSub);
        verifyNoMoreInteractions(userClient);
        verifyNoInteractions(companyService, productService, hubValidator);
    }

    @Test
    @DisplayName("실패 - 상품 검색 범위 조건이 잘못되면 입력값 오류로 처리한다")
    void searchProductsWithInvalidRange() {
        // given
        ProductSearchCondition condition = ProductSearchCondition.builder()
                .minPrice(20000)
                .maxPrice(10000)
                .build();

        // when
        Throwable throwable = catchThrowable(() ->
                productFacade.searchProducts(UUID.randomUUID(), condition, PageRequest.of(0, 10), "MASTER", null, null)
        );

        // then
        assertInvalidInput(throwable);
        verifyNoInteractions(userClient, companyService, productService, hubValidator);
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

    private ProductUpdateRequestDto createUpdateRequest() {
        ProductUpdateRequestDto request = new ProductUpdateRequestDto();
        ReflectionTestUtils.setField(request, "name", "수정 상품");
        ReflectionTestUtils.setField(request, "price", 15000);
        ReflectionTestUtils.setField(request, "stockQuantity", 30);
        return request;
    }

    private ApiResponse<UserResponseDto> createUserResponse(UUID companyId) {
        return createUserResponse(companyId, null);
    }

    private ApiResponse<UserResponseDto> createUserResponse(UUID companyId, UUID userId) {
        UserResponseDto user = new UserResponseDto();
        ReflectionTestUtils.setField(user, "id", userId);
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

