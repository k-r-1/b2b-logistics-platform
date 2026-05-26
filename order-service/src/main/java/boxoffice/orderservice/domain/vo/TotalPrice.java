package boxoffice.orderservice.domain.vo;

import boxoffice.orderservice.infra.exception.OrderDomainErrorCode;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TotalPrice {

  @Column(name = "price", nullable = false)
  private Integer value;

  public static TotalPrice create(Integer value) {
    validate(value);
    TotalPrice price = new TotalPrice();
    price.value = value;
    return price;
  }

  private static void validate(Integer value) {
    if (value == null || value < 0)
      throw new BaseException(OrderDomainErrorCode.INVALID_PRICE);
  }
}