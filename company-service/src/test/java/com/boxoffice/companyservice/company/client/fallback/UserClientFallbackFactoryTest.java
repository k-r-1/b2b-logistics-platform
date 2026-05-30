package com.boxoffice.companyservice.company.client.fallback;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.companyservice.company.client.UserClient;
import com.boxoffice.companyservice.company.client.dto.UserCompanyUpdateRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class UserClientFallbackFactoryTest {

    @Test
    @DisplayName("User fallback은 담당자 업체 매핑 실패를 Feign 실패 예외로 변환한다")
    void updateUserCompanyThrowsFeignClientError() {
        UserClient fallback = new UserClientFallbackFactory()
                .create(new RuntimeException("user-service unavailable"));

        Throwable throwable = catchThrowable(() ->
                fallback.updateUserCompany(UUID.randomUUID(), new UserCompanyUpdateRequestDto(UUID.randomUUID()))
        );

        assertFeignClientError(throwable);
    }

    @Test
    @DisplayName("User fallback은 Keycloak sub 조회 실패를 Feign 실패 예외로 변환한다")
    void getUserByKeycloakSubThrowsFeignClientError() {
        UserClient fallback = new UserClientFallbackFactory()
                .create(new RuntimeException("user-service unavailable"));

        Throwable throwable = catchThrowable(() -> fallback.getUserByKeycloakSub(UUID.randomUUID().toString()));

        assertFeignClientError(throwable);
    }

    private void assertFeignClientError(Throwable throwable) {
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FEIGN_CLIENT_ERROR));
    }
}
