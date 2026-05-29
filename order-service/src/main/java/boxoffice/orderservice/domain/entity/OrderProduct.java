package boxoffice.orderservice.domain.entity;

import boxoffice.orderservice.infra.exception.OrderDomainErrorCode;
import com.boxoffice.common.exception.BaseException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderProduct{

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(name = "unit_price", nullable = false)
  private Integer unitPrice;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  public static OrderProduct create(UUID productId, String productName, Integer unitPrice, Integer quantity) {
    validate(productId, productName, unitPrice, quantity);
    return new OrderProduct(productId, productName, unitPrice, quantity);
  }

  private static void validate(UUID productId, String productName, Integer unitPrice, Integer quantity) {
    if (productId == null || productName == null || productName.isEmpty()) {
      throw new BaseException(OrderDomainErrorCode.INVALID_PRODUCT_ID);
    }
    if (unitPrice == null || unitPrice < 0) {
      throw new BaseException(OrderDomainErrorCode.INVALID_ORDER_PRODUCT);
    }
    if (quantity == null || quantity < 0) {
      throw new BaseException(OrderDomainErrorCode.INVALID_ORDER_PRODUCT);
    }
  }
}