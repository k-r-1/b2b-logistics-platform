package com.boxoffice.companyservice.company.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.companyservice.company.domain.CompanyUserRole;
import com.boxoffice.companyservice.company.dto.request.CompanyCreateRequestDto;
import com.boxoffice.companyservice.company.dto.response.CompanyCreateResponseDto;
import com.boxoffice.companyservice.company.dto.response.CompanyResponseDto;
import com.boxoffice.companyservice.company.dto.search.CompanySearchCondition;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.boxoffice.companyservice.company.validator.HubValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyFacade {

    private final HubValidator hubValidator;
    private final CompanyService companyService;

    public CompanyCreateResponseDto createCompany(CompanyCreateRequestDto request, String userRoleStr, UUID userHubId) {
        validateCreateRequest(request);
        CompanyUserRole role = validateCompanyCreatePermission(userRoleStr);
        validateHubManagerCanCreateCompany(request.getHubId(), role, userHubId);
        // Feign 기반 허브 검증은 외부 응답 대기 시간을 트랜잭션 범위에 포함하지 않도록 트랜잭션 전에 수행한다.
        hubValidator.validateHubActive(request.getHubId());
        return companyService.createCompany(request);
    }

    public CompanyResponseDto getCompany(UUID companyId, String userRoleStr) {
        validateCompanyReadPermission(userRoleStr);
        return companyService.getCompany(companyId);
    }

    public Page<CompanyResponseDto> searchCompanies(CompanySearchCondition condition, Pageable pageable, String userRoleStr) {
        validateCompanyReadPermission(userRoleStr);
        
        CompanyType parsedType = null;
        if (condition != null && condition.getType() != null && !condition.getType().isBlank()) {
            try {
                parsedType = CompanyType.valueOf(condition.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BaseException(CommonErrorCode.INVALID_INPUT);
            }
        }
        
        return companyService.searchCompanies(condition, parsedType, pageable);
    }

    private void validateCreateRequest(CompanyCreateRequestDto request) {
        if (request == null || request.getHubId() == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private CompanyUserRole validateCompanyCreatePermission(String userRoleStr) {
        if (userRoleStr == null || userRoleStr.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        CompanyUserRole role = CompanyUserRole.fromString(userRoleStr);
        boolean canCreateCompany = (role == CompanyUserRole.MASTER) || (role == CompanyUserRole.HUB_MANAGER);

        if (!canCreateCompany) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        return role;
    }

    private void validateCompanyReadPermission(String userRoleStr) {
        if (userRoleStr == null || userRoleStr.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        CompanyUserRole role = CompanyUserRole.fromString(userRoleStr);
        boolean canReadCompany = role == CompanyUserRole.MASTER
                || role == CompanyUserRole.HUB_MANAGER
                || role == CompanyUserRole.DELIVERY_MANAGER
                || role == CompanyUserRole.SUPPLIER_MANAGER;

        if (!canReadCompany) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void validateHubManagerCanCreateCompany(UUID requestHubId, CompanyUserRole role, UUID userHubId) {
        if (role != CompanyUserRole.HUB_MANAGER) {
            return;
        }

        // 허브 관리자는 Gateway가 전달한 담당 허브 ID와 요청 허브 ID가 일치할 때만 업체를 생성할 수 있다.
        if (userHubId == null || !userHubId.equals(requestHubId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }
}
