package boxoffice.orderservice.domain.enums;

import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum OrderStatus {
  PENDING("주문 접수"),
  CONFIRMED("배송 요청 완료"),
  CANCELLED("주문 취소");

  private final String description;

  private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
      PENDING, Set.of(CONFIRMED, CANCELLED),
      CONFIRMED, Set.of(),
      CANCELLED, Set.of()
  );

  public boolean canTransitionTo(OrderStatus next) {
    return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
  }
}