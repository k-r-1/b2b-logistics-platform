package com.boxoffice.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정 클래스.
 * AuditorAwareImpl을 통해 created_by, updated_by를
 * 현재 인증된 사용자 ID로 자동 세팅한다.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaConfig {
}