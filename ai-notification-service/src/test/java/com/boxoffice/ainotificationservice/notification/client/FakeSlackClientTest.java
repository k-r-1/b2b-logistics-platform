package com.boxoffice.ainotificationservice.notification.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FakeSlackClient")
class FakeSlackClientTest {

    @Test
    @DisplayName("성공 - send 호출 시 success 응답, request 기록")
    void send_returns_success_and_records() {
        // given
        FakeSlackClient client = new FakeSlackClient();
        SendRequest req = new SendRequest(Recipient.user("U1"), "hello");

        // when
        SendResult result = client.send(req);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.httpStatusOptional()).contains(200);
        assertThat(client.recordedRequests()).containsExactly(req);
    }
}
