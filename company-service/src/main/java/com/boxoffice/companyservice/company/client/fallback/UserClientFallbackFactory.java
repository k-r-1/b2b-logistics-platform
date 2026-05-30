package com.boxoffice.companyservice.company.client.fallback;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.company.client.UserClient;
import com.boxoffice.companyservice.company.client.dto.UserCompanyUpdateRequestDto;
import com.boxoffice.companyservice.company.client.dto.UserResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public ApiResponse<Void> updateUserCompany(UUID userId, UserCompanyUpdateRequestDto request) {
                log.error("User service company mapping call failed. userId={}", userId, cause);
                throw new BaseException(CommonErrorCode.FEIGN_CLIENT_ERROR);
            }

            @Override
            public ApiResponse<UserResponseDto> getUserByKeycloakSub(String keycloakSub) {
                log.error("User service keycloak lookup call failed. keycloakSub={}", keycloakSub, cause);
                throw new BaseException(CommonErrorCode.FEIGN_CLIENT_ERROR);
            }
        };
    }
}
