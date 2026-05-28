package com.boxoffice.companyservice.company.service;

import com.boxoffice.companyservice.company.client.UserClient;
import com.boxoffice.companyservice.company.client.dto.UserCompanyUpdateRequestDto;
import com.boxoffice.companyservice.company.dto.request.CompanyCreateRequestDto;
import com.boxoffice.companyservice.company.dto.response.CompanyCreateResponseDto;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserClient userClient;

    @Transactional
    public CompanyCreateResponseDto createCompany(CompanyCreateRequestDto request) {
        Company company = Company.create(
                request.getName(),
                request.getType(),
                request.getHubId(),
                request.getAddress().toAddressVO()
        );

        Company savedCompany = companyRepository.save(company);

        if (request.getManagerUserId() != null) {
            // 담당자 역할 검증은 user-service가 수행하고, 실패 시 업체 생성 트랜잭션도 롤백한다.
            userClient.updateUserCompany(
                    request.getManagerUserId(),
                    new UserCompanyUpdateRequestDto(savedCompany.getId())
            );
        }

        log.info("Company created. companyId={}, type={}, hubId={}",
                savedCompany.getId(), savedCompany.getType(), savedCompany.getHubId());

        return CompanyCreateResponseDto.from(savedCompany);
    }
}
