package com.boxoffice.user_service.common.config;

import com.boxoffice.user_service.common.context.UserContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing // Audit 기능 활성화
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // 데이터가 저장/수정될 때 UserContextHolder에 저장된 유저명을 자동으로 물리틀에 매핑
        return () -> Optional.ofNullable(UserContextHolder.getUsername() != null ? UserContextHolder.getUsername() : "SYSTEM");
    }
}