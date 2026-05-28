package com.boxoffice.hubservice.hubroute.entity;

import com.boxoffice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "p_hub_routes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"origin_hub_id", "destination_hub_id"})
)
@Getter
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HubRoute extends BaseEntity {

    @Column(name = "origin_hub_id", nullable = false)
    private UUID originHubId;

    @Column(name = "destination_hub_id", nullable = false)
    private UUID destinationHubId;

    @Column(name = "estimated_duration_min", nullable = false)
    private Integer estimatedDurationMin;

    @Column(name = "estimated_distance_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal estimatedDistanceKm;

    @Builder
    private HubRoute(UUID originHubId, UUID destinationHubId,
                     Integer estimatedDurationMin, BigDecimal estimatedDistanceKm) {
        this.originHubId = originHubId;
        this.destinationHubId = destinationHubId;
        this.estimatedDurationMin = estimatedDurationMin;
        this.estimatedDistanceKm = estimatedDistanceKm;
    }

    public void update(Integer estimatedDurationMin, BigDecimal estimatedDistanceKm) {
        if (estimatedDurationMin != null) this.estimatedDurationMin = estimatedDurationMin;
        if (estimatedDistanceKm != null) this.estimatedDistanceKm = estimatedDistanceKm;
    }
}
