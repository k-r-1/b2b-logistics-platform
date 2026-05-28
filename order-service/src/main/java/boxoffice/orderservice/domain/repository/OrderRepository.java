package boxoffice.orderservice.domain.repository;

import boxoffice.orderservice.domain.entity.Order;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID>, OrderRepositoryCustom {

}
