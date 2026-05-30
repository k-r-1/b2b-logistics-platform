package com.boxoffice.companyservice.company.client.fallback;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.companyservice.company.client.HubClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HubClientFallbackFactoryTest {

    @Test
    @DisplayName("Hub fallback은 허브 호출 실패를 Feign 실패 예외로 변환한다")
    void checkHubActiveThrowsFeignClientError() {
        HubClientFallbackFactory fallbackFactory = new HubClientFallbackFactory();
        HubClient fallback = fallbackFactory.create(new RuntimeException("hub-service unavailable"));

        Throwable throwable = catchThrowable(() -> fallback.checkHubActive(UUID.randomUUID()));

        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FEIGN_CLIENT_ERROR));
    }
}
