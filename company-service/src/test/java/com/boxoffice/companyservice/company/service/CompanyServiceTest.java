package com.boxoffice.companyservice.company.service;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.companyservice.company.client.UserClient;
import com.boxoffice.companyservice.company.client.dto.UserCompanyUpdateRequestDto;
import com.boxoffice.companyservice.company.dto.request.AddressRequestDto;
import com.boxoffice.companyservice.company.dto.request.CompanyCreateRequestDto;
import com.boxoffice.companyservice.company.dto.request.CompanyUpdateRequestDto;
import com.boxoffice.companyservice.company.dto.response.CompanyCreateResponseDto;
import com.boxoffice.companyservice.company.dto.response.CompanyResponseDto;
import com.boxoffice.companyservice.company.dto.search.CompanySearchCondition;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.boxoffice.companyservice.company.exception.CompanyErrorCode;
import com.boxoffice.companyservice.company.repository.CompanyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyService 테스트")
class CompanyServiceTest {

    @InjectMocks
    private CompanyService companyService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserClient userClient;

    @Test
    @DisplayName("성공 - 요청을 업체 엔티티로 변환해 저장하고 담당자에 companyId를 매핑한다")
    void createCompanyConvertsRequestToEntitySavesAndMapsManagerUser() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();
        CompanyCreateRequestDto request = createRequest(
                "테스트 업체",
                CompanyType.SUPPLIER,
                hubId,
                managerUserId,
                "12345",
                "경기도 고양시 덕양구 권율대로 570",
                "101호"
        );

        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            ReflectionTestUtils.setField(company, "id", companyId);
            return company;
        });

        // when
        CompanyCreateResponseDto response = companyService.createCompany(request);

        // then
        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        ArgumentCaptor<UserCompanyUpdateRequestDto> userRequestCaptor =
                ArgumentCaptor.forClass(UserCompanyUpdateRequestDto.class);
        InOrder inOrder = inOrder(companyRepository, userClient);
        inOrder.verify(companyRepository).save(companyCaptor.capture());
        inOrder.verify(userClient).updateUserCompany(eq(managerUserId), userRequestCaptor.capture());
        verifyNoMoreInteractions(companyRepository, userClient);

        Company savedCompany = companyCaptor.getValue();
        assertThat(savedCompany.getName()).isEqualTo("테스트 업체");
        assertThat(savedCompany.getType()).isEqualTo(CompanyType.SUPPLIER);
        assertThat(savedCompany.getHubId()).isEqualTo(hubId);
        assertThat(savedCompany.isDeleted()).isFalse();
        assertThat(savedCompany.getDeletedAt()).isNull();

        AddressVO savedAddress = savedCompany.getAddress();
        assertThat(savedAddress.getZipCode()).isEqualTo("12345");
        assertThat(savedAddress.getAddress()).isEqualTo("경기도 고양시 덕양구 권율대로 570");
        assertThat(savedAddress.getDetailAddress()).isEqualTo("101호");

        assertThat(response.getCompanyId()).isEqualTo(companyId);
        assertThat(response.getName()).isEqualTo("테스트 업체");
        assertThat(response.getType()).isEqualTo(CompanyType.SUPPLIER);
        assertThat(response.getHubId()).isEqualTo(hubId);
        assertThat(response.getAddress().getZipCode()).isEqualTo("12345");
        assertThat(response.getAddress().getAddress()).isEqualTo("경기도 고양시 덕양구 권율대로 570");
        assertThat(response.getAddress().getDetailAddress()).isEqualTo("101호");
        assertThat(userRequestCaptor.getValue().companyId()).isEqualTo(companyId);
    }

    @Test
    @DisplayName("실패 - user-service 업체 담당자 매핑이 실패하면 예외를 전파한다")
    void createCompanyPropagatesUserCompanyMappingFailure() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();
        CompanyCreateRequestDto request = createRequest(
                "테스트 업체",
                CompanyType.SUPPLIER,
                hubId,
                managerUserId,
                "12345",
                "경기도 고양시 덕양구 권율대로 570",
                "101호"
        );
        RuntimeException mappingException = new RuntimeException("user-service mapping failed");

        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            ReflectionTestUtils.setField(company, "id", companyId);
            return company;
        });
        doThrow(mappingException)
                .when(userClient)
                .updateUserCompany(eq(managerUserId), any(UserCompanyUpdateRequestDto.class));

        // when
        Throwable throwable = catchThrowable(() -> companyService.createCompany(request));

        // then
        assertThat(throwable).isSameAs(mappingException);
        verify(companyRepository).save(any(Company.class));
        verify(userClient).updateUserCompany(eq(managerUserId), any(UserCompanyUpdateRequestDto.class));
        verifyNoMoreInteractions(companyRepository, userClient);
    }

    @Test
    @DisplayName("성공 - 담당자 ID가 없으면 업체만 생성하고 user-service 매핑을 호출하지 않는다")
    void createCompanyWithoutManagerUserIdDoesNotCallUserService() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        CompanyCreateRequestDto request = createRequest(
                "테스트 업체",
                CompanyType.SUPPLIER,
                hubId,
                null,
                "12345",
                "경기도 고양시 덕양구 권율대로 570",
                "101호"
        );

        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            ReflectionTestUtils.setField(company, "id", companyId);
            return company;
        });

        // when
        CompanyCreateResponseDto response = companyService.createCompany(request);

        // then
        assertThat(response.getCompanyId()).isEqualTo(companyId);
        verify(companyRepository).save(any(Company.class));
        verify(userClient, never()).updateUserCompany(any(), any(UserCompanyUpdateRequestDto.class));
        verifyNoMoreInteractions(companyRepository, userClient);
    }

    @Test
    @DisplayName("성공 - 업체 ID로 업체를 조회하고 상세 응답으로 반환한다")
    void getCompanyReturnsCompanyResponse() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Company company = createCompany(companyId, hubId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        // when
        CompanyResponseDto response = companyService.getCompany(companyId);

        // then
        verify(companyRepository).findById(companyId);
        verifyNoMoreInteractions(companyRepository);

        assertThat(response.getCompanyId()).isEqualTo(companyId);
        assertThat(response.getName()).isEqualTo("테스트 업체");
        assertThat(response.getType()).isEqualTo(CompanyType.SUPPLIER);
        assertThat(response.getHubId()).isEqualTo(hubId);
        assertThat(response.getAddress().getZipCode()).isEqualTo("12345");
        assertThat(response.getAddress().getAddress()).isEqualTo("경기도 고양시 덕양구 권율대로 570");
        assertThat(response.getAddress().getDetailAddress()).isEqualTo("101호");
    }

    @Test
    @DisplayName("실패 - 업체 ID에 해당하는 업체가 없으면 not found 예외를 반환한다")
    void getCompanyWithUnknownCompanyIdThrowsNotFound() {
        // given
        UUID companyId = UUID.randomUUID();
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // when
        Throwable throwable = catchThrowable(() -> companyService.getCompany(companyId));

        // then
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CompanyErrorCode.COMPANY_NOT_FOUND));
        verify(companyRepository).findById(companyId);
        verifyNoMoreInteractions(companyRepository);
    }

    @Test
    @DisplayName("성공 - 검색 조건과 페이지로 업체를 검색하고 응답 DTO 페이지를 반환한다")
    void searchCompaniesReturnsCompanyResponsePage() {
        // given
        CompanySearchCondition condition = new CompanySearchCondition("테스트", "SUPPLIER", UUID.randomUUID());
        Pageable pageable = PageRequest.of(0, 10);
        Company company = createCompany(UUID.randomUUID(), condition.getHubId());
        Page<Company> companyPage = new PageImpl<>(List.of(company), pageable, 1);

        when(companyRepository.searchCompanies(condition, CompanyType.SUPPLIER, pageable)).thenReturn(companyPage);

        // when
        Page<CompanyResponseDto> responsePage = companyService.searchCompanies(condition, CompanyType.SUPPLIER, pageable);

        // then
        verify(companyRepository).searchCompanies(condition, CompanyType.SUPPLIER, pageable);
        verifyNoMoreInteractions(companyRepository);

        assertThat(responsePage.getContent()).hasSize(1);
        assertThat(responsePage.getContent().get(0).getName()).isEqualTo("테스트 업체");
    }

    @Test
    @DisplayName("성공 - 업체 수정은 트랜잭션 내부에서 업체를 다시 조회한 뒤 변경한다")
    void updateCompanyFindsManagedEntityAndUpdatesProvidedFields() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Company company = createCompany(companyId, hubId);
        CompanyUpdateRequestDto request = createUpdateRequest(
                "수정 업체",
                CompanyType.RECEIVER,
                createAddressRequest("54321", "수정 주소", "수정 상세")
        );

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        // when
        companyService.updateCompany(companyId, request);

        // then
        verify(companyRepository).findById(companyId);
        verifyNoMoreInteractions(companyRepository);
        assertThat(company.getName()).isEqualTo("수정 업체");
        assertThat(company.getType()).isEqualTo(CompanyType.RECEIVER);
        assertThat(company.getHubId()).isEqualTo(hubId);
        assertThat(company.getAddress().getZipCode()).isEqualTo("54321");
        assertThat(company.getAddress().getAddress()).isEqualTo("수정 주소");
        assertThat(company.getAddress().getDetailAddress()).isEqualTo("수정 상세");
    }

    @Test
    @DisplayName("실패 - 수정 대상 업체가 없으면 not found 예외를 반환한다")
    void updateCompanyWithUnknownCompanyIdThrowsNotFound() {
        // given
        UUID companyId = UUID.randomUUID();
        CompanyUpdateRequestDto request = createUpdateRequest("수정 업체", null, null);
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // when
        Throwable throwable = catchThrowable(() -> companyService.updateCompany(companyId, request));

        // then
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CompanyErrorCode.COMPANY_NOT_FOUND));
        verify(companyRepository).findById(companyId);
        verifyNoMoreInteractions(companyRepository);
    }

    @Test
    @DisplayName("성공 - 업체를 soft delete 처리하고 삭제자를 기록한다")
    void deleteCompanySoftDeletesCompanyWithDeletedBy() {
        UUID companyId = UUID.randomUUID();
        UUID deletedBy = UUID.randomUUID();
        Company company = createCompany(companyId, UUID.randomUUID());
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        companyService.deleteCompany(companyId, deletedBy);

        verify(companyRepository).findById(companyId);
        verifyNoMoreInteractions(companyRepository);
        assertThat(company.isDeleted()).isTrue();
        assertThat(company.getDeletedAt()).isNotNull();
        assertThat(company.getDeletedBy()).isEqualTo(deletedBy);
    }

    @Test
    @DisplayName("실패 - 삭제 대상 업체가 없으면 not found 예외를 반환한다")
    void deleteCompanyWithUnknownCompanyIdThrowsNotFound() {
        UUID companyId = UUID.randomUUID();
        UUID deletedBy = UUID.randomUUID();
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        Throwable throwable = catchThrowable(() -> companyService.deleteCompany(companyId, deletedBy));

        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CompanyErrorCode.COMPANY_NOT_FOUND));
        verify(companyRepository).findById(companyId);
        verifyNoMoreInteractions(companyRepository);
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

    // setter 없는 요청 DTO를 테스트 입력값으로 구성한다.
    private CompanyCreateRequestDto createRequest(
            String name,
            CompanyType type,
            UUID hubId,
            UUID managerUserId,
            String zipCode,
            String address,
            String detailAddress
    ) {
        AddressRequestDto addressRequest = new AddressRequestDto();
        ReflectionTestUtils.setField(addressRequest, "zipCode", zipCode);
        ReflectionTestUtils.setField(addressRequest, "address", address);
        ReflectionTestUtils.setField(addressRequest, "detailAddress", detailAddress);

        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "type", type);
        ReflectionTestUtils.setField(request, "hubId", hubId);
        ReflectionTestUtils.setField(request, "managerUserId", managerUserId);
        ReflectionTestUtils.setField(request, "address", addressRequest);
        return request;
    }

    private AddressRequestDto createAddressRequest(String zipCode, String address, String detailAddress) {
        AddressRequestDto addressRequest = new AddressRequestDto();
        ReflectionTestUtils.setField(addressRequest, "zipCode", zipCode);
        ReflectionTestUtils.setField(addressRequest, "address", address);
        ReflectionTestUtils.setField(addressRequest, "detailAddress", detailAddress);
        return addressRequest;
    }

    private CompanyUpdateRequestDto createUpdateRequest(
            String name,
            CompanyType type,
            AddressRequestDto address
    ) {
        CompanyUpdateRequestDto request = createEmptyUpdateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "type", type);
        ReflectionTestUtils.setField(request, "address", address);
        return request;
    }

    private CompanyUpdateRequestDto createEmptyUpdateRequest() {
        try {
            Constructor<CompanyUpdateRequestDto> constructor = CompanyUpdateRequestDto.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("CompanyUpdateRequestDto 테스트 객체를 생성할 수 없습니다.", e);
        }
    }
}
