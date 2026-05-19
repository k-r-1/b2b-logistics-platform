package com.boxoffice.user_service.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

@Configuration
@EnableJpaAuditing // 1. 여기서 Audit 기능을 확실하게 활성화합니다.
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // 2. 스프링 시큐리티가 토큰 검증 후 보관 중인 인증 객체를 꺼냅니다.
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 3. 인증 정보가 없거나, 익명 사용자인 경우 시스템(SYSTEM) 계정으로 기본 처리합니다.
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return Optional.of("SYSTEM");
            }

            // 4. Keycloak JWT 토큰이 확인되면 토큰 안에서 유저의 ID 또는 이메일(preferred_username)을 추출합니다.
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                return Optional.ofNullable(jwt.getClaimAsString("preferred_username"));
            }

            return Optional.of(authentication.getName());
        };
    }
}