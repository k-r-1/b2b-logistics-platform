package com.boxoffice.companyservice.company.validator;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.company.client.HubClient;
import com.boxoffice.companyservice.company.client.dto.HubActiveResponseDto;
import com.boxoffice.companyservice.company.exception.CompanyErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubValidator {

    private final HubClient hubClient;

    public void validateHubActive(UUID hubId) {
        try {
            ApiResponse<HubActiveResponseDto> wrapper = hubClient.checkHubActive(hubId);

            if (wrapper == null || wrapper.getData() == null) {
                throw new BaseException(CommonErrorCode.FEIGN_CLIENT_ERROR);
            }

            if (!wrapper.getData().isActive()) {
                throw new BaseException(CompanyErrorCode.HUB_INACTIVE);
            }

        } catch (FeignException.NotFound e) {
            throw new BaseException(CompanyErrorCode.HUB_NOT_FOUND);
        } catch (FeignException e) {
            log.error("Hub Service call failed for hubId: {}", hubId, e);
            throw new BaseException(CommonErrorCode.FEIGN_CLIENT_ERROR);
        }
    }
}
