package com.boxoffice.companyservice.company.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.company.client.UserClient;
import com.boxoffice.companyservice.company.client.dto.UserResponseDto;
import com.boxoffice.companyservice.company.domain.CompanyUserRole;
import com.boxoffice.companyservice.company.dto.request.CompanyCreateRequestDto;
import com.boxoffice.companyservice.company.dto.request.CompanyUpdateRequestDto;
import com.boxoffice.companyservice.company.dto.response.CompanyCreateResponseDto;
import com.boxoffice.companyservice.company.dto.response.CompanyResponseDto;
import com.boxoffice.companyservice.company.dto.search.CompanySearchCondition;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.boxoffice.companyservice.company.validator.HubValidator;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyFacade {

    private final HubValidator hubValidator;
    private final UserClient userClient;
    private final CompanyService companyService;
    private final AuditorAware<UUID> auditorAware;

    public CompanyCreateResponseDto createCompany(CompanyCreateRequestDto request, String userRoleStr, UUID userHubId) {
        validateCreateRequest(request);
        CompanyUserRole role = validateCompanyCreatePermission(userRoleStr);
        validateHubManagerCanCreateCompany(request.getHubId(), role, userHubId);
        // Feign 기반 허브 검증은 DB 트랜잭션 밖에서 수행한다.
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

    public void updateCompany(UUID companyId, CompanyUpdateRequestDto request, String userRoleStr, UUID userHubId, String keycloakSub) {
        validateUpdateRequest(companyId, request);
        CompanyUserRole role = validateCompanyUpdatePermission(userRoleStr);
        Company company = companyService.getCompanyEntity(companyId);

        validateCompanyUpdateScope(company, role, userHubId, keycloakSub);

        companyService.updateCompany(companyId, request);
    }

    public void deleteCompany(UUID companyId, String userRoleStr, UUID userHubId, String keycloakSub) {
        validateDeleteRequest(companyId, keycloakSub);
        CompanyUserRole role = validateCompanyDeletePermission(userRoleStr);
        Company company = companyService.getCompanyEntity(companyId);

        validateCompanyDeleteScope(company, role, userHubId);

        UUID deletedBy = auditorAware.getCurrentAuditor()
                .orElseThrow(() -> new BaseException(CommonErrorCode.UNAUTHORIZED));
        companyService.deleteCompany(companyId, deletedBy);
    }

    private void validateCreateRequest(CompanyCreateRequestDto request) {
        if (request == null || request.getHubId() == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private void validateUpdateRequest(UUID companyId, CompanyUpdateRequestDto request) {
        if (companyId == null || request == null || !request.hasUpdateField() || request.hasBlankName()) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private void validateDeleteRequest(UUID companyId, String keycloakSub) {
        if (companyId == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }

        if (keycloakSub == null || keycloakSub.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private CompanyUserRole validateCompanyCreatePermission(String userRoleStr) {
        if (userRoleStr == null || userRoleStr.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        CompanyUserRole role = CompanyUserRole.fromString(userRoleStr);
        boolean canCreateCompany = role == CompanyUserRole.MASTER || role == CompanyUserRole.HUB_MANAGER;

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

    private CompanyUserRole validateCompanyUpdatePermission(String userRoleStr) {
        if (userRoleStr == null || userRoleStr.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        CompanyUserRole role = CompanyUserRole.fromString(userRoleStr);
        boolean canUpdateCompany = role == CompanyUserRole.MASTER
                || role == CompanyUserRole.HUB_MANAGER
                || role == CompanyUserRole.SUPPLIER_MANAGER;

        if (!canUpdateCompany) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        return role;
    }

    private CompanyUserRole validateCompanyDeletePermission(String userRoleStr) {
        if (userRoleStr == null || userRoleStr.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        CompanyUserRole role = CompanyUserRole.fromString(userRoleStr);
        boolean canDeleteCompany = role == CompanyUserRole.MASTER || role == CompanyUserRole.HUB_MANAGER;

        if (!canDeleteCompany) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        return role;
    }

    private void validateCompanyUpdateScope(Company company, CompanyUserRole role, UUID userHubId, String keycloakSub) {
        if (role == CompanyUserRole.MASTER) {
            return;
        }

        if (role == CompanyUserRole.HUB_MANAGER) {
            validateHubManagerCanUpdateCompany(company, userHubId);
            return;
        }

        if (role == CompanyUserRole.SUPPLIER_MANAGER) {
            validateSupplierManagerCanUpdateCompany(company.getId(), keycloakSub);
        }
    }

    private void validateCompanyDeleteScope(Company company, CompanyUserRole role, UUID userHubId) {
        if (role == CompanyUserRole.MASTER) {
            return;
        }

        if (role == CompanyUserRole.HUB_MANAGER) {
            validateHubManagerCanDeleteCompany(company, userHubId);
        }
    }

    private void validateHubManagerCanCreateCompany(UUID requestHubId, CompanyUserRole role, UUID userHubId) {
        if (role != CompanyUserRole.HUB_MANAGER) {
            return;
        }

        if (userHubId == null || !userHubId.equals(requestHubId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void validateHubManagerCanUpdateCompany(Company company, UUID userHubId) {
        if (userHubId == null || !userHubId.equals(company.getHubId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void validateHubManagerCanDeleteCompany(Company company, UUID userHubId) {
        if (userHubId == null || !userHubId.equals(company.getHubId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void validateSupplierManagerCanUpdateCompany(UUID companyId, String keycloakSub) {
        if (keycloakSub == null || keycloakSub.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        ApiResponse<UserResponseDto> response = getUserByKeycloakSub(keycloakSub);
        if (response == null || response.getData() == null || response.getData().getCompanyId() == null) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        if (!companyId.equals(response.getData().getCompanyId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private ApiResponse<UserResponseDto> getUserByKeycloakSub(String keycloakSub) {
        try {
            return userClient.getUserByKeycloakSub(keycloakSub);
        } catch (FeignException e) {
            throw new BaseException(CommonErrorCode.FEIGN_CLIENT_ERROR);
        }
    }
}
