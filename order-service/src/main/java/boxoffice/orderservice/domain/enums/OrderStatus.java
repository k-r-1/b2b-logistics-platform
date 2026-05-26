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

}