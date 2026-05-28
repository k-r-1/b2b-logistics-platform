package boxoffice.deliveryservice.domain.deliveryroute.repository;

import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {
}
