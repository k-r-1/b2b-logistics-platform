package com.boxoffice.ainotificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// @CreatedDate 등 JPA Auditing 활성화. 미설정 시 created_at이 채워지지 않아 NOT NULL 위반.
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

}
