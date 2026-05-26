package com.boxoffice.ainotificationservice.notification.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

// 실제 Slack 어댑터 도입 전 placeholder. 항상 success 응답 + 요청 기록.
@Component
public class FakeSlackClient implements NotificationClient {

    private static final SendResult DEFAULT_RESULT = SendResult.success(200, Duration.ofMillis(10));

    private final List<SendRequest> requestHistory = new ArrayList<>();

    @Override
    public SendResult send(SendRequest request) {
        requestHistory.add(request);
        return DEFAULT_RESULT;
    }

    public List<SendRequest> recordedRequests() {
        return List.copyOf(requestHistory);
    }
}
