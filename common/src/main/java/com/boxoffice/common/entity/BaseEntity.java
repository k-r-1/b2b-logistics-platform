package com.boxoffice.common.entity;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 모든 엔티티의 공통 필드를 관리하는 추상 클래스.
 *
 * UUID v7 기반 PK를 자동 생성하며
 * JPA Auditing을 통해 생성/수정 일시와 생성자/수정자를 자동으로 기록한다.
 * Soft Delete를 지원하며 deleted_at이 null이면 활성 상태이다.
 * 각 서비스 엔티티에서 id 선언 불필요.
 * 저장 전에 id가 필요한 경우 assignId() 사용.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    private static final TimeBasedEpochGenerator UUID_GENERATOR =
            Generators.timeBasedEpochGenerator();

    /** PK. UUID v7 자동 생성. */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** 생성 일시. 최초 저장 시 자동 세팅. 이후 변경 불가. */
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /** 생성자 UUID. 최초 저장 시 자동 세팅. */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    /** 수정 일시. 변경 시마다 자동 갱신. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 수정자 UUID. 변경 시마다 자동 갱신. */
    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    /** 삭제 일시. null이면 활성 상태. Soft Delete 시 현재 시각으로 세팅. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 삭제자 UUID. Soft Delete 시 세팅. */
    @Column(name = "deleted_by")
    private UUID deletedBy;

    /**
     * 저장 전에 id를 미리 세팅해야 하는 경우 사용한다.
     * 다른 서비스에 id를 전달해야 할 때 저장 전에 호출.
     * id가 이미 있으면 변경하지 않는다.
     *
     * 사용 예시:
     *   Hub hub = new Hub();
     *   hub.assignId(UUID_GENERATOR.generate());
     *   UUID hubId = hub.getId(); // 저장 전에 id 알 수 있음
     *   hubRepository.save(hub); // @PrePersist에서 id 건너뜀
     *
     * @param id 미리 세팅할 UUID v7
     */
    protected void assignId(UUID id) {
        if (this.id == null) {
            this.id = id;
        }
    }

    /**
     * 최초 저장 시 UUID v7 기반 PK를 자동 생성한다.
     * assignId()로 미리 세팅한 경우 건너뜀.
     */
    @PrePersist
    protected void prePersist() {
        if (id == null) {
            id = UUID_GENERATOR.generate();
        }
    }

    /**
     * 논리적 삭제(Soft Delete)를 수행한다.
     * deleted_at을 현재 시각으로, deleted_by를 전달받은 사용자 UUID로 세팅한다.
     *
     * @param deletedBy 삭제를 수행한 사용자 UUID
     */
    public void softDelete(UUID deletedBy) {
        if (isDeleted()) return;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    /**
     * 논리적 삭제 여부를 반환한다.
     *
     * @return deleted_at이 null이 아니면 true
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}