package com.boxoffice.delivery_manager_service.entity;

import com.boxoffice.delivery_manager_service.common.IdGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "p_delivery_managers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// ★ BaseEntity 상속 추가
public class DeliveryManager extends BaseEntity {

    @Id
    @Column(name = "manager_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "slack_id", nullable = false)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ManagerStatus status;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = IdGenerator.createUUIDv7();
        }
    }

    @Builder
    public DeliveryManager(UUID userId, String slackId, ManagerStatus status) {
        this.userId = userId;
        this.slackId = slackId;
        this.status = status;
    }
}