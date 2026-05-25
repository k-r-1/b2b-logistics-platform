package com.boxoffice.deliverymanagerservice.repository;

import com.boxoffice.deliverymanagerservice.entity.DeliveryManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {

    Optional<DeliveryManager> findByUserId(UUID userId);

    Page<DeliveryManager> findByHubId(UUID hubId, Pageable pageable);
}