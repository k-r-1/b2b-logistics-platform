package com.boxoffice.hubservice.hub.entity;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_hubs")
@Getter
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hub extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Embedded
    private AddressVO address;

    @Embedded
    private CoordinateVO coordinate;

    @Enumerated(EnumType.STRING)
    @Column(name = "hub_type", nullable = false, length = 20)
    private HubType hubType;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "closing_reason", length = 500)
    private String closingReason;

    @Column(name = "capacity")
    private Integer capacity;

    @Builder
    private Hub(String name, AddressVO address, CoordinateVO coordinate, HubType hubType, Integer capacity) {
        this.name = name;
        this.address = address;
        this.coordinate = coordinate;
        this.hubType = hubType;
        this.capacity = capacity;
    }

    public void update(String name, AddressVO address, CoordinateVO coordinate, Integer capacity) {
        if (name != null) {
            this.name = name;
        }
        if (address != null) {
            this.address = address;
        }
        if (coordinate != null) {
            this.coordinate = coordinate;
        }
        if (capacity != null) {
            this.capacity = capacity;
        }
    }

    public void assignManager(UUID managerId) {
        this.managerId = managerId;
    }

    public void startClosing(String reason) {
        this.hubType = HubType.CLOSING;
        this.closingReason = reason;
    }

    public boolean isClosing() {
        return this.hubType == HubType.CLOSING;
    }

    // p_hub_routes는 여기서 삭제하지 않음. 진행 중 배송 완료 후 DELETE 2단계에서 처리
    public void deactivate() {
        this.hubType = HubType.INACTIVE;
    }

    public boolean isInactive() {
        return this.hubType == HubType.INACTIVE;
    }
}
