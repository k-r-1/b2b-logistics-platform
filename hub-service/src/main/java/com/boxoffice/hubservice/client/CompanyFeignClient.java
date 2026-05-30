package com.boxoffice.hubservice.client;

import com.boxoffice.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "company-service", fallbackFactory = CompanyFeignClientFallbackFactory.class)
public interface CompanyFeignClient {

    @GetMapping("/internal/v1/companies")
    ApiResponse<List<CompanyDetailResponseDto>> getCompaniesByHubId(@RequestParam("hubId") UUID hubId);

    @PatchMapping("/internal/v1/companies/bulk-hub-transfer")
    ApiResponse<Void> bulkHubTransfer(@RequestBody BulkHubTransferRequestDto request);

    @PostMapping("/internal/v1/products/hubs/stock-counts")
    ApiResponse<List<BulkStockCountResponseDto>> getBulkStockCount(@RequestBody BulkStockCountRequestDto request);
}

