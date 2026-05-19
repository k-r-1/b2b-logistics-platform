package com.boxoffice.delivery_manager_service.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

@Configuration
@EnableJpaAuditing // JPA Auditing 기능 활성화
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // 1. 게이트웨이를 통과해 스프링 시큐리티에 저장된 인증 객체를 가져옵니다.
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 2. 인증 정보가 없거나 비로그인 사용자인 경우 시스템(SYSTEM)으로 기록합니다.
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return Optional.of("SYSTEM");
            }

            // 3. Keycloak이 준 JWT 토큰이 확인되면 유저의 preferred_username(ID 또는 이메일)을 추출합니다.
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                return Optional.ofNullable(jwt.getClaimAsString("preferred_username"));
            }

            return Optional.of(authentication.getName());
        };
    }
}