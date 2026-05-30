package com.boxoffice.hubservice.client;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class CompanyFeignClientFallbackFactory implements FallbackFactory<CompanyFeignClient> {

    @Override
    public CompanyFeignClient create(Throwable cause) {
        log.error("company-service 호출 실패: {}", cause.getMessage());
        return new CompanyFeignClient() {

            @Override
            public ApiResponse<List<CompanyDetailResponseDto>> getCompaniesByHubId(UUID hubId) {
                throw new BaseException(CommonErrorCode.FEIGN_CLIENT_ERROR);
            }

            @Override
            public ApiResponse<Void> bulkHubTransfer(BulkHubTransferRequestDto request) {
                throw new BaseException(CommonErrorCode.FEIGN_CLIENT_ERROR);
            }

            @Override
            public ApiResponse<List<BulkStockCountResponseDto>> getBulkStockCount(BulkStockCountRequestDto request) {
                throw new BaseException(CommonErrorCode.FEIGN_CLIENT_ERROR);
            }
        };
    }
}
