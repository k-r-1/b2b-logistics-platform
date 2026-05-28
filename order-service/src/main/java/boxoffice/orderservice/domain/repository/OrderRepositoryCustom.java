package boxoffice.orderservice.domain.repository;

import boxoffice.orderservice.domain.entity.Order;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryCustom {

    Optional<Order> findByIdWithProducts(UUID orderId);
}
