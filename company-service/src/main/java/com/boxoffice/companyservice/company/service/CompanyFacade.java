package com.boxoffice.companyservice.company.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.companyservice.company.dto.request.CompanyCreateRequestDto;
import com.boxoffice.companyservice.company.dto.response.CompanyCreateResponseDto;
import com.boxoffice.companyservice.company.validator.HubValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyFacade {

    private static final String ROLE_MASTER = "MASTER";
    private static final String ROLE_HUB_MANAGER = "HUB_MANAGER";

    private final HubValidator hubValidator;
    private final CompanyService companyService;

    public CompanyCreateResponseDto createCompany(CompanyCreateRequestDto request, String userRole, UUID userHubId) {
        String normalizedRole = validateCompanyCreatePermission(userRole);
        validateHubManagerCanCreateCompany(request.getHubId(), normalizedRole, userHubId);
        // Feign 기반 허브 검증은 외부 응답 대기 시간을 트랜잭션 범위에 포함하지 않도록 트랜잭션 전에 수행한다.
        hubValidator.validateHubActive(request.getHubId());
        return companyService.createCompany(request);
    }

    private String validateCompanyCreatePermission(String userRole) {
        if (userRole == null || userRole.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        // Role enum이 common에 확정되기 전까지는 Gateway가 전달한 헤더 문자열을 정규화해 비교한다.
        String normalizedRole = userRole.trim().toUpperCase(Locale.ROOT);
        boolean canCreateCompany = ROLE_MASTER.equals(normalizedRole)
                || ROLE_HUB_MANAGER.equals(normalizedRole);

        if (!canCreateCompany) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        return normalizedRole;
    }

    private void validateHubManagerCanCreateCompany(UUID requestHubId, String normalizedRole, UUID userHubId) {
        if (!ROLE_HUB_MANAGER.equals(normalizedRole)) {
            return;
        }

        // 허브 관리자는 Gateway가 전달한 담당 허브 ID와 요청 허브 ID가 일치할 때만 업체를 생성할 수 있다.
        if (userHubId == null || !userHubId.equals(requestHubId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }
}
