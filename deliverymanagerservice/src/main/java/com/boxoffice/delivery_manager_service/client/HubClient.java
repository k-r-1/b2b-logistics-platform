package com.boxoffice.delivery_manager_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

// 팀원이 유레카에 등록할 서비스 이름이 'hub-service'라고 가정하고 연결
@FeignClient(name = "hub-service", path = "/api/v1/hubs")
public interface HubClient {

    /**
     * 팀원의 허브 서비스에 존재하는 허브인지 단건 조회하여 검증하는 API 호출 채널
     */
    @GetMapping("/{hubId}/check")
    boolean checkHubExists(@PathVariable("hubId") UUID hubId);
}