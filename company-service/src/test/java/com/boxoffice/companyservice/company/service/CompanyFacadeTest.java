package com.boxoffice.companyservice.company.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.companyservice.company.dto.request.CompanyCreateRequestDto;
import com.boxoffice.companyservice.company.dto.response.CompanyCreateResponseDto;
import com.boxoffice.companyservice.company.exception.CompanyErrorCode;
import com.boxoffice.companyservice.company.validator.HubValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyFacade 테스트")
class CompanyFacadeTest {

    @InjectMocks
    private CompanyFacade companyFacade;

    @Mock
    private HubValidator hubValidator;

    @Mock
    private CompanyService companyService;

    @Test
    @DisplayName("실패 - 생성 요청이 없으면 입력값 오류로 처리하고 Hub 검증과 저장을 호출하지 않는다")
    void createCompanyWithoutRequest() {
        // when
        Throwable throwable = catchThrowable(() -> companyFacade.createCompany(null, "MASTER", null));

        // then
        assertInvalidInput(throwable);
        verifyNoInteractions(hubValidator, companyService);
    }

    @Test
    @DisplayName("실패 - 요청 hubId가 없으면 입력값 오류로 처리하고 Hub 검증과 저장을 호출하지 않는다")
    void createCompanyWithoutRequestHubId() {
        // given
        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        // when
        Throwable throwable = catchThrowable(() -> companyFacade.createCompany(request, "MASTER", null));

        // then
        assertInvalidInput(throwable);
        verifyNoInteractions(hubValidator, companyService);
    }

    @Test
    @DisplayName("성공 - MASTER는 해당 허브 헤더 없이 업체를 생성한다")
    void createCompanyWithMasterRole() {
        // given
        CompanyCreateRequestDto request = createRequest(UUID.randomUUID());
        CompanyCreateResponseDto expectedResponse = mock(CompanyCreateResponseDto.class);
        when(companyService.createCompany(request)).thenReturn(expectedResponse);

        // when
        CompanyCreateResponseDto response = companyFacade.createCompany(request, "MASTER", null);

        // then
        assertThat(response).isSameAs(expectedResponse);
        verifyCreateOrder(request);
    }

    @DisplayName("성공 - HUB_MANAGER는 해당 허브가 일치하면 업체를 생성한다")
    @ParameterizedTest(name = "role={0}")
    @ValueSource(strings = {"HUB_MANAGER", " hub_manager "})
    void createCompanyWithHubManagerRoleAndSameHub(String userRole) {
        // given
        // " hub_manager "는 공백/소문자가 섞인 role도 정규화되어 허용되는지 확인하는 케이스다.
        UUID hubId = UUID.randomUUID();
        CompanyCreateRequestDto request = createRequest(hubId);
        CompanyCreateResponseDto expectedResponse = mock(CompanyCreateResponseDto.class);
        when(companyService.createCompany(request)).thenReturn(expectedResponse);

        // when
        CompanyCreateResponseDto response = companyFacade.createCompany(request, userRole, hubId);

        // then
        assertThat(response).isSameAs(expectedResponse);
        verifyCreateOrder(request);
    }

    @Test
    @DisplayName("실패 - HUB_MANAGER는 해당 허브 헤더가 없으면 업체를 생성할 수 없다")
    void createCompanyWithHubManagerRoleAndMissingUserHubId() {
        // given
        CompanyCreateRequestDto request = createRequest(UUID.randomUUID());

        // when
        Throwable throwable = catchThrowable(() -> companyFacade.createCompany(request, "HUB_MANAGER", null));

        // then
        assertForbidden(throwable);
        verifyNoInteractions(hubValidator, companyService);
    }

    @Test
    @DisplayName("실패 - HUB_MANAGER는 해당 허브와 요청 허브가 다르면 업체를 생성할 수 없다")
    void createCompanyWithHubManagerRoleAndDifferentHub() {
        // given
        CompanyCreateRequestDto request = createRequest(UUID.randomUUID());
        UUID userHubId = UUID.randomUUID();

        // when
        Throwable throwable = catchThrowable(() -> companyFacade.createCompany(request, "HUB_MANAGER", userHubId));

        // then
        assertForbidden(throwable);
        verifyNoInteractions(hubValidator, companyService);
    }

    @Test
    @DisplayName("실패 - Hub 검증이 실패하면 예외를 전파하고 업체를 저장하지 않는다")
    void createCompanyWhenHubValidationFails() {
        // given
        CompanyCreateRequestDto request = createRequest(UUID.randomUUID());
        BaseException hubException = new BaseException(CompanyErrorCode.HUB_INACTIVE);
        // 권한 통과 후 Hub 검증에서 실패하면 DB 저장 단계로 넘어가면 안 된다.
        doThrow(hubException)
                .when(hubValidator)
                .validateHubActive(request.getHubId());

        // when
        Throwable throwable = catchThrowable(() -> companyFacade.createCompany(request, "MASTER", null));

        // then
        assertThat(throwable).isSameAs(hubException);
        verify(hubValidator).validateHubActive(request.getHubId());
        verifyNoMoreInteractions(hubValidator);
        verifyNoInteractions(companyService);
    }

    @Test
    @DisplayName("실패 - role 헤더가 없으면 인증 실패로 처리하고 Hub 검증과 저장을 호출하지 않는다")
    void createCompanyWithoutRole() {
        // given
        CompanyCreateRequestDto request = createRequest(UUID.randomUUID());

        // when
        Throwable throwable = catchThrowable(() -> companyFacade.createCompany(request, null, null));

        // then
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED));
        verifyNoInteractions(hubValidator, companyService);
    }

    @Test
    @DisplayName("실패 - role 헤더가 공백이면 인증 실패로 처리하고 Hub 검증과 저장을 호출하지 않는다")
    void createCompanyWithBlankRole() {
        // given
        CompanyCreateRequestDto request = createRequest(UUID.randomUUID());

        // when
        Throwable throwable = catchThrowable(() -> companyFacade.createCompany(request, "   ", null));

        // then
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED));
        verifyNoInteractions(hubValidator, companyService);
    }

    @DisplayName("실패 - 업체 생성 권한이 없는 role은 업체를 생성할 수 없다")
    @ParameterizedTest(name = "role={0}")
    @ValueSource(strings = {"DELIVERY_MANAGER", "SUPPLIER_MANAGER", "UNKNOWN_ROLE"})
    void createCompanyWithForbiddenRole(String userRole) {
        // given
        CompanyCreateRequestDto request = createRequest(UUID.randomUUID());

        // when
        Throwable throwable = catchThrowable(() -> companyFacade.createCompany(request, userRole, null));

        // then
        assertForbidden(throwable);
        verifyNoInteractions(hubValidator, companyService);
    }

    private void verifyCreateOrder(CompanyCreateRequestDto request) {
        // Facade는 권한과 담당 허브를 먼저 확인한 뒤 Hub 검증, 저장 순서로 진행해야 한다.
        InOrder inOrder = inOrder(hubValidator, companyService);
        inOrder.verify(hubValidator).validateHubActive(request.getHubId());
        inOrder.verify(companyService).createCompany(request);
        verifyNoMoreInteractions(hubValidator, companyService);
    }

    private void assertForbidden(Throwable throwable) {
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    private void assertInvalidInput(Throwable throwable) {
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    private CompanyCreateRequestDto createRequest(UUID hubId) {
        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        ReflectionTestUtils.setField(request, "hubId", hubId);
        ReflectionTestUtils.setField(request, "managerUserId", UUID.randomUUID());
        return request;
    }
}
