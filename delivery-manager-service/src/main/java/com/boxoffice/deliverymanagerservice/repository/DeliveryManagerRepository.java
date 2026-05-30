package com.boxoffice.deliverymanagerservice.repository;

import com.boxoffice.deliverymanagerservice.entity.DeliveryManager;
import com.boxoffice.deliverymanagerservice.entity.DeliveryType;
import com.boxoffice.deliverymanagerservice.entity.ManagerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {

    Optional<DeliveryManager> findByUserId(UUID userId);

    Page<DeliveryManager> findByHubId(UUID hubId, Pageable pageable);

    Optional<DeliveryManager> findFirstByHubIdAndTypeAndStatusAndDeletedAtIsNullOrderByLastAssignedAtAsc(
            UUID hubId,
            DeliveryType type,
            ManagerStatus status
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DeliveryManager dm SET dm.hubId = null, dm.status = :status WHERE dm.hubId = :hubId")
    void clearHubIdAndChangeStatusByHubId(@Param("hubId") UUID hubId, @Param("status") ManagerStatus status);

    @Query("SELECT dm FROM DeliveryManager dm WHERE " +
            "(:hubId IS NULL OR dm.hubId = :hubId) AND " +
            "(:type IS NULL OR dm.type = :type) AND " +
            "(:status IS NULL OR dm.status = :status) AND " +
            "dm.deletedAt IS NULL")
    Page<DeliveryManager> searchManagers(@Param("hubId") UUID hubId,
                                         @Param("type") DeliveryType type,
                                         @Param("status") ManagerStatus status,
                                         Pageable pageable);

}