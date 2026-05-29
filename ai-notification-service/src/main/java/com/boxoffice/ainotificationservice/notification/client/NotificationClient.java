package com.boxoffice.ainotificationservice.notification.client;

public interface NotificationClient {

    SendResult send(SendRequest request);
}
