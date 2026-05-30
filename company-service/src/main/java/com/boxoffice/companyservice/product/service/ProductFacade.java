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
import com.boxoffice.companyservice.product.dto.request.ProductUpdateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import com.boxoffice.companyservice.product.dto.response.ProductResponseDto;
import com.boxoffice.companyservice.product.dto.search.ProductSearchCondition;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductFacade {

    private final UserClient userClient;
    private final CompanyService companyService;
    private final ProductService productService;
    private final HubValidator hubValidator;
    private final AuditorAware<UUID> auditorAware;

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

    public ProductResponseDto getProduct(
            UUID companyId,
            UUID productId,
            String userRoleStr,
            UUID userHubId,
            String keycloakSub
    ) {
        validateGetRequest(companyId, productId);
        CompanyUserRole role = validateProductReadPermission(userRoleStr);
        validateSupplierManagerKeycloakSub(role, keycloakSub);
        Company company = companyService.getCompanyEntity(companyId);

        validateProductReadScope(company, role, userHubId, keycloakSub);

        return productService.getProduct(companyId, productId);
    }

    public void updateProduct(
            UUID companyId,
            UUID productId,
            ProductUpdateRequestDto request,
            String userRoleStr,
            UUID userHubId,
            String keycloakSub
    ) {
        validateUpdateRequest(companyId, productId, request);
        CompanyUserRole role = validateProductUpdatePermission(userRoleStr);
        validateSupplierManagerKeycloakSub(role, keycloakSub);
        Company company = companyService.getCompanyEntity(companyId);

        validateProductUpdateScope(company, role, userHubId, keycloakSub);
        // Product는 company.hubId 기준으로 관리되므로 비활성 허브 소속 상품 수정은 막는다.
        hubValidator.validateHubActive(company.getHubId());
        productService.updateProduct(companyId, productId, request);
    }

    public void deleteProduct(
            UUID companyId,
            UUID productId,
            String userRoleStr,
            UUID userHubId,
            String keycloakSub
    ) {
        validateDeleteRequest(companyId, productId, keycloakSub);
        CompanyUserRole role = validateProductDeletePermission(userRoleStr);
        Company company = companyService.getCompanyEntity(companyId);

        validateProductDeleteScope(company, role, userHubId);
        UUID deletedBy = auditorAware.getCurrentAuditor()
                .orElseThrow(() -> new BaseException(CommonErrorCode.UNAUTHORIZED));
        productService.deleteProduct(companyId, productId, deletedBy);
    }

    public Page<ProductResponseDto> searchProducts(
            UUID companyId,
            ProductSearchCondition condition,
            Pageable pageable,
            String userRoleStr,
            UUID userHubId,
            String keycloakSub
    ) {
        validateSearchRequest(companyId, condition, pageable);
        CompanyUserRole role = validateProductReadPermission(userRoleStr);
        validateSupplierManagerKeycloakSub(role, keycloakSub);
        Company company = companyService.getCompanyEntity(companyId);

        validateProductReadScope(company, role, userHubId, keycloakSub);

        return productService.searchProducts(companyId, condition, pageable);
    }

    public Page<ProductResponseDto> searchProducts(
            ProductSearchCondition condition,
            Pageable pageable,
            String userRoleStr,
            UUID userHubId,
            String keycloakSub
    ) {
        ProductSearchCondition validCondition = validateSearchRequest(condition, pageable);
        CompanyUserRole role = validateProductReadPermission(userRoleStr);
        validateSupplierManagerKeycloakSub(role, keycloakSub);

        applyProductSearchScope(validCondition, role, userHubId, keycloakSub);

        return productService.searchProducts(validCondition, pageable);
    }

    private void validateCreateRequest(UUID companyId, ProductCreateRequestDto request) {
        if (companyId == null || request == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private void validateGetRequest(UUID companyId, UUID productId) {
        if (companyId == null || productId == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private void validateUpdateRequest(UUID companyId, UUID productId, ProductUpdateRequestDto request) {
        if (companyId == null || productId == null || request == null
                || !request.hasUpdateField() || request.hasBlankName()) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private void validateDeleteRequest(UUID companyId, UUID productId, String keycloakSub) {
        if (companyId == null || productId == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }

        if (keycloakSub == null || keycloakSub.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private void validateSearchRequest(UUID companyId, ProductSearchCondition condition, Pageable pageable) {
        if (companyId == null || pageable == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }

        validateSearchCondition(condition);
    }

    private ProductSearchCondition validateSearchRequest(ProductSearchCondition condition, Pageable pageable) {
        if (pageable == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }

        ProductSearchCondition validCondition = condition == null ? new ProductSearchCondition() : condition;
        validateSearchCondition(validCondition);
        return validCondition;
    }

    private void validateSearchCondition(ProductSearchCondition condition) {
        if (condition == null) {
            return;
        }

        boolean hasNegativeCondition = isNegative(condition.getMinPrice())
                || isNegative(condition.getMaxPrice())
                || isNegative(condition.getMinStockQuantity())
                || isNegative(condition.getMaxStockQuantity());
        boolean hasInvalidPriceRange = condition.getMinPrice() != null
                && condition.getMaxPrice() != null
                && condition.getMinPrice() > condition.getMaxPrice();
        boolean hasInvalidStockRange = condition.getMinStockQuantity() != null
                && condition.getMaxStockQuantity() != null
                && condition.getMinStockQuantity() > condition.getMaxStockQuantity();

        if (hasNegativeCondition || hasInvalidPriceRange || hasInvalidStockRange) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private boolean isNegative(Integer value) {
        return value != null && value < 0;
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

    private CompanyUserRole validateProductReadPermission(String userRoleStr) {
        if (userRoleStr == null || userRoleStr.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        CompanyUserRole role = CompanyUserRole.fromString(userRoleStr);
        boolean canReadProduct = role == CompanyUserRole.MASTER
                || role == CompanyUserRole.HUB_MANAGER
                || role == CompanyUserRole.DELIVERY_MANAGER
                || role == CompanyUserRole.SUPPLIER_MANAGER;

        if (!canReadProduct) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        return role;
    }

    private CompanyUserRole validateProductUpdatePermission(String userRoleStr) {
        if (userRoleStr == null || userRoleStr.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        CompanyUserRole role = CompanyUserRole.fromString(userRoleStr);
        boolean canUpdateProduct = role == CompanyUserRole.MASTER
                || role == CompanyUserRole.HUB_MANAGER
                || role == CompanyUserRole.SUPPLIER_MANAGER;

        if (!canUpdateProduct) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        return role;
    }

    private CompanyUserRole validateProductDeletePermission(String userRoleStr) {
        if (userRoleStr == null || userRoleStr.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        CompanyUserRole role = CompanyUserRole.fromString(userRoleStr);
        boolean canDeleteProduct = role == CompanyUserRole.MASTER
                || role == CompanyUserRole.HUB_MANAGER;

        if (!canDeleteProduct) {
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
            validateHubManagerCanAccessProduct(company, userHubId);
            return;
        }

        if (role == CompanyUserRole.SUPPLIER_MANAGER) {
            validateSupplierManagerCanAccessCompany(company.getId(), keycloakSub);
        }
    }

    private void validateProductReadScope(
            Company company,
            CompanyUserRole role,
            UUID userHubId,
            String keycloakSub
    ) {
        if (role == CompanyUserRole.HUB_MANAGER) {
            validateHubManagerCanAccessProduct(company, userHubId);
            return;
        }

        if (role == CompanyUserRole.SUPPLIER_MANAGER) {
            validateSupplierManagerCanAccessCompany(company.getId(), keycloakSub);
        }
    }

    private void validateProductUpdateScope(
            Company company,
            CompanyUserRole role,
            UUID userHubId,
            String keycloakSub
    ) {
        if (role == CompanyUserRole.MASTER) {
            return;
        }

        if (role == CompanyUserRole.HUB_MANAGER) {
            validateHubManagerCanAccessProduct(company, userHubId);
            return;
        }

        if (role == CompanyUserRole.SUPPLIER_MANAGER) {
            validateSupplierManagerCanAccessCompany(company.getId(), keycloakSub);
        }
    }

    private void validateProductDeleteScope(Company company, CompanyUserRole role, UUID userHubId) {
        if (role == CompanyUserRole.MASTER) {
            return;
        }

        if (role == CompanyUserRole.HUB_MANAGER) {
            validateHubManagerCanAccessProduct(company, userHubId);
        }
    }

    private void applyProductSearchScope(
            ProductSearchCondition condition,
            CompanyUserRole role,
            UUID userHubId,
            String keycloakSub
    ) {
        if (role == CompanyUserRole.HUB_MANAGER) {
            if (userHubId == null) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
            if (condition.getHubId() != null && !condition.getHubId().equals(userHubId)) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
            condition.setHubId(userHubId);
            return;
        }

        if (role == CompanyUserRole.SUPPLIER_MANAGER) {
            UUID supplierCompanyId = resolveSupplierManagerCompanyId(keycloakSub);
            if (condition.getCompanyId() != null && !condition.getCompanyId().equals(supplierCompanyId)) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
            condition.setCompanyId(supplierCompanyId);
        }
    }

    private void validateHubManagerCanAccessProduct(Company company, UUID userHubId) {
        if (userHubId == null || !userHubId.equals(company.getHubId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void validateSupplierManagerCanAccessCompany(UUID companyId, String keycloakSub) {
        UUID supplierCompanyId = resolveSupplierManagerCompanyId(keycloakSub);
        if (!companyId.equals(supplierCompanyId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private UUID resolveSupplierManagerCompanyId(String keycloakSub) {
        try {
            ApiResponse<UserResponseDto> response = userClient.getUserByKeycloakSub(keycloakSub);
            if (response == null || response.getData() == null || response.getData().getCompanyId() == null) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }

            return response.getData().getCompanyId();
        } catch (FeignException e) {
            throw new BaseException(CommonErrorCode.FEIGN_CLIENT_ERROR);
        }
    }

}
