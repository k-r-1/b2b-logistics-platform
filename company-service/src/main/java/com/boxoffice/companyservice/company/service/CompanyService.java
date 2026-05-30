package com.boxoffice.companyservice.company.service;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.companyservice.company.client.UserClient;
import com.boxoffice.companyservice.company.client.dto.UserCompanyUpdateRequestDto;
import com.boxoffice.companyservice.company.dto.request.CompanyCreateRequestDto;
import com.boxoffice.companyservice.company.dto.request.CompanyUpdateRequestDto;
import com.boxoffice.companyservice.company.dto.response.CompanyCreateResponseDto;
import com.boxoffice.companyservice.company.dto.response.CompanyResponseDto;
import com.boxoffice.companyservice.company.dto.search.CompanySearchCondition;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.boxoffice.companyservice.company.exception.CompanyErrorCode;
import com.boxoffice.companyservice.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

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

    @Transactional(readOnly = true)
    public CompanyResponseDto getCompany(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BaseException(CompanyErrorCode.COMPANY_NOT_FOUND));

        return CompanyResponseDto.from(company);
    }

    @Transactional(readOnly = true)
    public Company getCompanyEntity(UUID companyId) {
        // Facade 권한 검증에서 대상 업체의 현재 소속 허브와 식별자를 확인하기 위한 내부 조회 메서드다.
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BaseException(CompanyErrorCode.COMPANY_NOT_FOUND));
    }

    @Transactional
    public void updateCompany(UUID companyId, CompanyUpdateRequestDto request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BaseException(CompanyErrorCode.COMPANY_NOT_FOUND));
        AddressVO address = request.getAddress() == null ? null : request.getAddress().toAddressVO();
        company.update(request.getName(), request.getType(), address);
        log.info("Company updated. companyId={}, name={}, type={}, addressChanged={}",
                company.getId(), company.getName(), company.getType(), request.getAddress() != null);
    }

    @Transactional
    public void deleteCompany(UUID companyId, UUID deletedBy) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BaseException(CompanyErrorCode.COMPANY_NOT_FOUND));
        company.softDelete(deletedBy);
        log.info("Company deleted. companyId={}, deletedBy={}", company.getId(), deletedBy);
    }

    @Transactional(readOnly = true)
    public List<Company> getCompaniesByHubId(UUID hubId) {
        return companyRepository.findActiveCompaniesByHubId(hubId);
    }

    @Transactional
    public void transferCompaniesHub(List<UUID> companyIds, UUID toHubId) {
        if (companyIds == null || companyIds.isEmpty() || toHubId == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }

        List<UUID> distinctCompanyIds = new LinkedHashSet<>(companyIds).stream().toList();
        long updatedCount = companyRepository.bulkUpdateHubId(distinctCompanyIds, toHubId);

        if (updatedCount != distinctCompanyIds.size()) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponseDto> searchCompanies(CompanySearchCondition condition, CompanyType type, Pageable pageable) {
        Page<Company> companies = companyRepository.searchCompanies(condition, type, pageable);
        return companies.map(CompanyResponseDto::from);
    }
}
