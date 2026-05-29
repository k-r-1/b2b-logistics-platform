package com.boxoffice.companyservice.common.config;

import com.boxoffice.common.config.AuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.UUID;

@Configuration
public class JpaAuditingConfig {

    @Bean(name = "auditorAwareImpl")
    public AuditorAware<UUID> auditorAwareImpl() {
        return new AuditorAwareImpl();
    }
}
