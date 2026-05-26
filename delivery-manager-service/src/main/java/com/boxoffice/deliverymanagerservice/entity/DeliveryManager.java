package com.boxoffice.deliverymanagerservice.entity;

import com.boxoffice.common.entity.BaseEntity;
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
public class DeliveryManager extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "slack_id", nullable = false)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ManagerStatus status;

    @Builder
    public DeliveryManager(UUID userId, String slackId, ManagerStatus status) {
        this.userId = userId;
        this.slackId = slackId;
        this.status = status;
    }
}