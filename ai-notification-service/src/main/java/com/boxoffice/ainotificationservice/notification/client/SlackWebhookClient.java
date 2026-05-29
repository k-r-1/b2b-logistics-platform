package com.boxoffice.ainotificationservice.notification.client;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

// Slack Incoming Webhook 실제 어댑터. 빈 구성은 SlackClientConfig가 webhook-url 설정 시 등록.
// 2xx=성공, 4xx=영구 실패(재시도 무의미), 5xx·네트워크 오류=일시 실패(재시도 가능).
@Slf4j
public class SlackWebhookClient implements NotificationClient {

    private final RestClient restClient;
    private final String webhookUrl;
    private final Clock clock;

    public SlackWebhookClient(RestClient restClient, String webhookUrl, Clock clock) {
        this.restClient = restClient;
        this.webhookUrl = webhookUrl;
        this.clock = clock;
    }

    @Override
    public SendResult send(SendRequest request) {
        long start = clock.millis();
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", request.body()))
                    .retrieve()
                    .toEntity(String.class);
            return SendResult.success(response.getStatusCode().value(), elapsed(start));
        } catch (HttpClientErrorException e) {
            log.warn("Slack webhook 영구 실패 status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return SendResult.permanentFailure(e.getStatusCode().value(), e.getResponseBodyAsString(), elapsed(start));
        } catch (HttpServerErrorException e) {
            log.warn("Slack webhook 일시 실패 status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return SendResult.transientFailure(e.getStatusCode().value(), e.getResponseBodyAsString(), elapsed(start));
        } catch (ResourceAccessException e) {
            log.warn("Slack webhook 네트워크 오류", e);
            return SendResult.transientFailure(null, e.getMessage(), elapsed(start));
        }
    }

    private Duration elapsed(long startMillis) {
        return Duration.ofMillis(clock.millis() - startMillis);
    }
}
