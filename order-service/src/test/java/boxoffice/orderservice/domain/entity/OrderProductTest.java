package boxoffice.orderservice.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import boxoffice.orderservice.infra.exception.OrderDomainErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("OrderProduct VO 단위 테스트")
class OrderProductTest {

  private final UUID validProductId = UUID.randomUUID();
  private final String validProductName = "오리지널 팝콘 (L)";
  private final Integer validUnitPrice = 6000;
  private final Integer validQuantity = 2;

  @Nested
  @DisplayName("create() 메서드는")
  class Describe_create {

    @Test
    @DisplayName("[성공] 유효한 파라미터가 주어지면 OrderProduct 객체를 생성한다.")
    void success_create_order_product() {
      // Given & When
      OrderProduct product = OrderProduct.create(
          validProductId,
          validProductName,
          validUnitPrice,
          validQuantity
      );

      // Then
      assertThat(product.getProductId()).isEqualTo(validProductId);
      assertThat(product.getProductName()).isEqualTo(validProductName);
      assertThat(product.getUnitPrice()).isEqualTo(validUnitPrice);
      assertThat(product.getQuantity()).isEqualTo(validQuantity);
    }

    @Test
    @DisplayName("[예외] productId가 null이면 예외를 발생시킨다.")
    void exception_when_productId_is_null() {
      // Given
      UUID nullProductId = null;

      // When & Then
      assertThatThrownBy(() -> OrderProduct.create(nullProductId, validProductName, validUnitPrice, validQuantity))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.INVALID_PRODUCT_ID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("[예외] productName이 null이거나 빈 문자열이면 예외를 발생시킨다.")
    void exception_when_productName_is_null_or_empty(String invalidProductName) {
      // Given (ParameterizedTest에서 null과 "" 주입)

      // When & Then
      assertThatThrownBy(() -> OrderProduct.create(validProductId, invalidProductName, validUnitPrice, validQuantity))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.INVALID_PRODUCT_ID);
    }

    @Test
    @DisplayName("[예외] unitPrice가 null이면 예외를 발생시킨다.")
    void exception_when_unitPrice_is_null() {
      // Given
      Integer nullUnitPrice = null;

      // When & Then
      assertThatThrownBy(() -> OrderProduct.create(validProductId, validProductName, nullUnitPrice, validQuantity))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.INVALID_ORDER_PRODUCT);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, -9999})
    @DisplayName("[예외] unitPrice가 음수이면 예외를 발생시킨다.")
    void exception_when_unitPrice_is_negative(Integer negativeUnitPrice) {
      // Given (ParameterizedTest에서 음수 값 주입)

      // When & Then
      assertThatThrownBy(() -> OrderProduct.create(validProductId, validProductName, negativeUnitPrice, validQuantity))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.INVALID_ORDER_PRODUCT);
    }

    @Test
    @DisplayName("[예외] quantity가 null이면 예외를 발생시킨다.")
    void exception_when_quantity_is_null() {
      // Given
      Integer nullQuantity = null;

      // When & Then
      assertThatThrownBy(() -> OrderProduct.create(validProductId, validProductName, validUnitPrice, nullQuantity))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.INVALID_ORDER_PRODUCT);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10, -50})
    @DisplayName("[예외] quantity가 음수이면 예외를 발생시킨다.")
    void exception_when_quantity_is_negative(Integer negativeQuantity) {
      // Given (ParameterizedTest에서 음수 값 주입)

      // When & Then
      assertThatThrownBy(() -> OrderProduct.create(validProductId, validProductName, validUnitPrice, negativeQuantity))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.INVALID_ORDER_PRODUCT);
    }
  }
}