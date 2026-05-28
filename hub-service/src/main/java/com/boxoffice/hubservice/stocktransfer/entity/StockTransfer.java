package com.boxoffice.hubservice.stocktransfer.entity;

import com.boxoffice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_stock_transfers")
@Getter
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockTransfer extends BaseEntity {

    @Column(name = "from_hub_id", nullable = false)
    private UUID fromHubId;

    @Column(name = "to_hub_id", nullable = false)
    private UUID toHubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "total_product_count", nullable = false)
    private Integer totalProductCount;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "note", length = 500)
    private String note;

    @Builder
    private StockTransfer(UUID fromHubId, UUID toHubId, Integer totalProductCount, UUID managerId) {
        this.fromHubId = fromHubId;
        this.toHubId = toHubId;
        this.totalProductCount = totalProductCount;
        this.managerId = managerId;
        this.status = TransferStatus.PENDING;
    }

    public void dispatch() {
        this.status = TransferStatus.IN_PROGRESS;
        this.dispatchedAt = LocalDateTime.now();
    }

    public void complete(String note) {
        this.status = TransferStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.note = note;
    }

    public void cancel() {
        this.status = TransferStatus.CANCELLED;
    }
}
