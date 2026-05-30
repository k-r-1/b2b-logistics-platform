package com.boxoffice.companyservice.company.client.fallback;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.company.client.HubClient;
import com.boxoffice.companyservice.company.client.dto.HubActiveResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class HubClientFallbackFactory implements FallbackFactory<HubClient> {

    @Override
    public HubClient create(Throwable cause) {
        return new HubClient() {
            @Override
            public ApiResponse<HubActiveResponseDto> checkHubActive(UUID hubId) {
                log.error("Hub service call failed. hubId={}", hubId, cause);
                throw new BaseException(CommonErrorCode.FEIGN_CLIENT_ERROR);
            }
        };
    }
}
