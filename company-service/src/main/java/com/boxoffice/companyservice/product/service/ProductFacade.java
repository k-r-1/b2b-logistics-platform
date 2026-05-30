package com.boxoffice.companyservice.product.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.company.client.UserClient;
import com.boxoffice.companyservice.company.client.dto.UserResponseDto;
import com.boxoffice.companyservice.company.domain.CompanyUserRole;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.service.CompanyService;
import com.boxoffice.companyservice.company.validator.HubValidator;
import com.boxoffice.companyservice.product.dto.request.ProductCreateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductFacade {

    private final UserClient userClient;
    private final CompanyService companyService;
    private final ProductService productService;
    private final HubValidator hubValidator;

    public ProductCreateResponseDto createProduct(
            UUID companyId,
            ProductCreateRequestDto request,
            String userRoleStr,
            UUID userHubId,
            String keycloakSub
    ) {
        validateCreateRequest(companyId, request);
        CompanyUserRole role = validateProductCreatePermission(userRoleStr);
        validateSupplierManagerKeycloakSub(role, keycloakSub);
        Company company = companyService.getCompanyEntity(companyId);

        validateProductCreateScope(company, role, userHubId, keycloakSub);
        hubValidator.validateHubActive(company.getHubId());

        return productService.createProduct(companyId, request);
    }

    private void validateCreateRequest(UUID companyId, ProductCreateRequestDto request) {
        if (companyId == null || request == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private CompanyUserRole validateProductCreatePermission(String userRoleStr) {
        if (userRoleStr == null || userRoleStr.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        CompanyUserRole role = CompanyUserRole.fromString(userRoleStr);
        boolean canCreateProduct = role == CompanyUserRole.MASTER
                || role == CompanyUserRole.HUB_MANAGER
                || role == CompanyUserRole.SUPPLIER_MANAGER;

        if (!canCreateProduct) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        return role;
    }

    private void validateSupplierManagerKeycloakSub(CompanyUserRole role, String keycloakSub) {
        if (role == CompanyUserRole.SUPPLIER_MANAGER && (keycloakSub == null || keycloakSub.isBlank())) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private void validateProductCreateScope(
            Company company,
            CompanyUserRole role,
            UUID userHubId,
            String keycloakSub
    ) {
        if (role == CompanyUserRole.MASTER) {
            return;
        }

        if (role == CompanyUserRole.HUB_MANAGER) {
            validateHubManagerCanCreateProduct(company, userHubId);
            return;
        }

        if (role == CompanyUserRole.SUPPLIER_MANAGER) {
            validateSupplierManagerCanCreateProduct(company.getId(), keycloakSub);
        }
    }

    private void validateHubManagerCanCreateProduct(Company company, UUID userHubId) {
        if (userHubId == null || !userHubId.equals(company.getHubId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void validateSupplierManagerCanCreateProduct(UUID companyId, String keycloakSub) {
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
