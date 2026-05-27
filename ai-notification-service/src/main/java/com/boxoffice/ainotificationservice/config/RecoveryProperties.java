package com.boxoffice.ainotificationservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.recovery")
public record RecoveryProperties(Duration staleAfter) {

    public RecoveryProperties {
        if (staleAfter == null || staleAfter.isNegative() || staleAfter.isZero()) {
            throw new IllegalArgumentException("staleAfter must be positive");
        }
    }
}
