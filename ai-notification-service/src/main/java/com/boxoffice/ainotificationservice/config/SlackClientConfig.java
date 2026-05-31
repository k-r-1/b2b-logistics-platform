package com.boxoffice.ainotificationservice.config;

import com.boxoffice.ainotificationservice.notification.client.FakeSlackClient;
import com.boxoffice.ainotificationservice.notification.client.NotificationClient;
import com.boxoffice.ainotificationservice.notification.client.SlackWebhookClient;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// Slack 어댑터 빈 선택: webhook-url 설정 시 실제 SlackWebhookClient, 없으면 FakeSlackClient로 폴백.
@Configuration
public class SlackClientConfig {

    @Bean
    @ConditionalOnExpression("'${notification.slack.webhook-url:}' != ''")
    public NotificationClient slackWebhookClient(
            @Value("${notification.slack.webhook-url}") String webhookUrl,
            Clock clock) {
        return new SlackWebhookClient(RestClient.create(), webhookUrl, clock);
    }

    @Bean
    @ConditionalOnMissingBean(NotificationClient.class)
    public NotificationClient fakeSlackClient() {
        return new FakeSlackClient();
    }
}
