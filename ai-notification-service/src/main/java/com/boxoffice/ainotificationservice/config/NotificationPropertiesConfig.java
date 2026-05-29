package com.boxoffice.ainotificationservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DispatchProperties.class, RecoveryProperties.class})
public class NotificationPropertiesConfig {

}
