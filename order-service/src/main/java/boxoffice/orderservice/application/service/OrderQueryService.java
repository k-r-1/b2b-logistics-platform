package boxoffice.orderservice.application.service;

import boxoffice.orderservice.application.service.dto.OrderResultDto;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.repository.OrderRepository;
import boxoffice.orderservice.infra.config.CacheConfig;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true)
    public Order findById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BaseException(OrderErrorCode.ORDER_NOT_FOUND));
        order.getOrderProducts().size();
        return order;
    }

    @Cacheable(value = CacheConfig.ORDER_CACHE, key = "#orderId")
    @Transactional(readOnly = true)
    public OrderResultDto findByIdAsDto(UUID orderId) {
        return Timer.builder("order.repository.findById")
            .description("orderRepository.findByIdWithProducts 쿼리 실행 시간")
            .register(meterRegistry)
            .record(() -> {
                Order order = orderRepository.findByIdWithProducts(orderId)
                    .orElseThrow(() -> new BaseException(OrderErrorCode.ORDER_NOT_FOUND));
                return OrderResultDto.from(order);
            });
    }

    @CacheEvict(value = CacheConfig.ORDER_CACHE, key = "#orderId")
    public void evictOrderCache(UUID orderId) {
    }
}
