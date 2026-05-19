package com.boxoffice.common.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * FeignClient 공통 설정 클래스.
 * 서비스 간 호출 시 Gateway가 주입한 사용자 정보 헤더를 자동으로 전달한다.
 */
@Configuration
public class FeignConfig {

    /**
     * FeignClient 요청 인터셉터를 등록한다.
     * 현재 요청의 사용자 정보 헤더를 다음 서비스 호출에 자동으로 포함시킨다.
     *
     * @return 요청 인터셉터
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String userId   = attrs.getRequest().getHeader("X-User-Id");
                String username = attrs.getRequest().getHeader("X-User-Username");
                String userRole = attrs.getRequest().getHeader("X-User-Role");

                if (userId != null)   requestTemplate.header("X-User-Id", userId);
                if (username != null) requestTemplate.header("X-User-Username", username);
                if (userRole != null) requestTemplate.header("X-User-Role", userRole);
            }
        };
    }

    /**
     * FeignClient 로그 레벨을 설정한다.
     * FULL: 요청/응답 헤더, 바디, 메타데이터 전체 로깅.
     *
     * @return 로그 레벨
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}