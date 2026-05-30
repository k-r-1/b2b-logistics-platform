package boxoffice.deliveryservice.domain.delivery.repository;


import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.delivery.entity.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Page<Delivery> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT d FROM Delivery d "
            + "WHERE (d.originHubId = :hubId OR d.destinationHubId = :hubId) "
            + "AND d.deletedAt IS NULL")
    Page<Delivery> findAllByHubIdAndDeletedAtIsNull(@Param("hubId") UUID hubId, Pageable pageable);

    Page<Delivery> findAllByDeliveryPersonIdAndDeletedAtIsNull(UUID deliveryPersonId, Pageable pageable);

    Page<Delivery> findAllByCompanyIdAndDeletedAtIsNull(UUID companyId, Pageable pageable);

    Optional<Delivery> findByOrderId(UUID orderId);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE (d.originHubId = :hubId OR d.destinationHubId = :hubId) AND d.deliveryStatus NOT IN :excludedStatuses AND d.deletedAt IS NULL")
    int countActiveByHubId(@Param("hubId") UUID hubId, @Param("excludedStatuses") List<DeliveryStatus> excludedStatuses);
}
