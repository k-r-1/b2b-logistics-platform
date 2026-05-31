package com.boxoffice.ainotificationservice.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("SlackWebhookClient")
class SlackWebhookClientTest {

    private static final String WEBHOOK_URL = "https://hooks.slack.com/services/T/B/X";

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneOffset.UTC);

    private MockRestServiceServer server;
    private SlackWebhookClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SlackWebhookClient(builder.build(), WEBHOOK_URL, clock);
    }

    @Test
    @DisplayName("성공 - 2xx 응답이면 success, 본문은 {\"text\":...} 로 전송")
    void send_success() {
        server.expect(requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.text").value("안녕하세요"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        SendResult result = client.send(new SendRequest(Recipient.channel("C1"), "안녕하세요"));

        assertThat(result.success()).isTrue();
        assertThat(result.httpStatusOptional()).contains(200);
        server.verify();
    }

    @Test
    @DisplayName("실패 - 4xx 응답이면 영구 실패")
    void send_4xx_permanent() {
        server.expect(requestTo(WEBHOOK_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("channel_not_found"));

        SendResult result = client.send(new SendRequest(Recipient.channel("C1"), "x"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureTypeOptional()).contains(FailureType.PERMANENT);
        assertThat(result.httpStatusOptional()).contains(404);
    }

    @Test
    @DisplayName("실패 - 5xx 응답이면 일시 실패")
    void send_5xx_transient() {
        server.expect(requestTo(WEBHOOK_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        SendResult result = client.send(new SendRequest(Recipient.channel("C1"), "x"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureTypeOptional()).contains(FailureType.TRANSIENT);
        assertThat(result.httpStatusOptional()).contains(500);
    }
}
