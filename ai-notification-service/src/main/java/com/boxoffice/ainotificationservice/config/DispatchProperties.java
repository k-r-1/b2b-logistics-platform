package com.boxoffice.ainotificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.dispatch")
public record DispatchProperties(int maxAttempts) {

    public DispatchProperties {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
    }
}
