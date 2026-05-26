package com.boxoffice.ainotificationservice.notification.repository;

import com.boxoffice.ainotificationservice.notification.entity.message.NotificationStatus;
import com.boxoffice.ainotificationservice.notification.entity.message.SlackMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlackMessageRepository extends JpaRepository<SlackMessage, UUID> {

    Optional<SlackMessage> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<SlackMessage> findAllByStatusAndCreatedAtBefore(NotificationStatus status, LocalDateTime threshold);
}
