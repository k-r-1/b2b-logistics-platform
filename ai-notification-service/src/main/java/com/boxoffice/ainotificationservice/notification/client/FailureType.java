package com.boxoffice.ainotificationservice.notification.client;

public enum FailureType {
    PERMANENT, // 재시도 무의미 (channel_not_found, invalid_auth)
    TRANSIENT  // 재시도 가능 (5xx, rate_limited)
}
