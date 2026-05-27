package com.boxoffice.deliverymanagerservice.entity;

import com.boxoffice.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_delivery_managers")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeliveryManager extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "hub_id", nullable = true)
    private UUID hubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    private DeliveryType type;

    @Column(name = "slack_id", nullable = true)
    private String slackId;

    @Column(name = "last_assigned_at")
    private LocalDateTime lastAssignedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ManagerStatus status;

    public void updateHub(UUID newHubId) {
        this.hubId = newHubId;
    }

    public void updateType(DeliveryType newType) {
        this.type = newType;
    }

    public void recordAssignment() {
        this.lastAssignedAt = LocalDateTime.now();
    }
}