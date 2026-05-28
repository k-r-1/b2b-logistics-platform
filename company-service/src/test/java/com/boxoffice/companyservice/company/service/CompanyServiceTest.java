package com.boxoffice.companyservice.company.service;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.companyservice.company.dto.request.AddressRequestDto;
import com.boxoffice.companyservice.company.dto.request.CompanyCreateRequestDto;
import com.boxoffice.companyservice.company.dto.response.CompanyCreateResponseDto;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.boxoffice.companyservice.company.repository.CompanyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyService 테스트")
class CompanyServiceTest {

    @InjectMocks
    private CompanyService companyService;

    @Mock
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("성공 - 요청을 업체 엔티티로 변환해 저장하고 생성 응답으로 반환한다")
    void createCompanyConvertsRequestToEntitySavesAndReturnsResponse() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        CompanyCreateRequestDto request = createRequest(
                "테스트 업체",
                CompanyType.SUPPLIER,
                hubId,
                "12345",
                "경기도 고양시 덕양구 권율대로 570",
                "101호"
        );

        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            // Repository save 이후 응답 DTO가 id를 사용할 수 있도록 저장된 엔티티 상태를 흉내 낸다.
            ReflectionTestUtils.setField(company, "id", companyId);
            return company;
        });

        // when
        CompanyCreateResponseDto response = companyService.createCompany(request);

        // then
        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(companyCaptor.capture());
        verifyNoMoreInteractions(companyRepository);

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
    }

    // setter 없는 요청 DTO를 테스트 입력값으로 구성한다.
    private CompanyCreateRequestDto createRequest(
            String name,
            CompanyType type,
            UUID hubId,
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
        ReflectionTestUtils.setField(request, "address", addressRequest);
        return request;
    }
}
