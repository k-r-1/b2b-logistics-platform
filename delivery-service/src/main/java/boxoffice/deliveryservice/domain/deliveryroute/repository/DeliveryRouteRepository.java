package boxoffice.deliveryservice.domain.deliveryroute.repository;

import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {

    Page<DeliveryRoute> findAllByDeliveryIdAndDeletedAtIsNull(UUID deliveryId, Pageable pageable);

    List<DeliveryRoute> findAllByDeliveryIdAndDeletedAtIsNull(UUID deliveryId);

    Optional<DeliveryRoute> findByIdAndDeliveryIdAndDeletedAtIsNull(UUID id, UUID deliveryId);
}
