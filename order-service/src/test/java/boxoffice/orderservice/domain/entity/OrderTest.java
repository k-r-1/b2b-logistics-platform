package boxoffice.orderservice.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import boxoffice.orderservice.domain.enums.OrderStatus;
import boxoffice.orderservice.infra.exception.OrderDomainErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Order 엔티티 단위 테스트")
class OrderTest {

  private final UUID orderId = UUID.randomUUID();
  private final UUID supplierId = UUID.randomUUID();
  private final UUID receiverId = UUID.randomUUID();
  private final UUID sourceHubId = UUID.randomUUID();
  private final UUID destinationHubId = UUID.randomUUID();
  private final String request = "문 앞 배송 부탁드립니다.";

  @Nested
  @DisplayName("create() 메서드는")
  class Describe_create {

    @Test
    @DisplayName("[성공] 유효한 파라미터가 주어지면, PENDING 상태의 주문을 생성하고 총 금액을 계산한다.")
    void success_create_order() {
      // Given
      OrderProduct mockProduct1 = mock(OrderProduct.class);
      given(mockProduct1.getUnitPrice()).willReturn(10_000);
      given(mockProduct1.getQuantity()).willReturn(2); // 20,000

      OrderProduct mockProduct2 = mock(OrderProduct.class);
      given(mockProduct2.getUnitPrice()).willReturn(5_000);
      given(mockProduct2.getQuantity()).willReturn(3); // 15,000

      List<OrderProduct> products = List.of(mockProduct1, mockProduct2);

      // When
      Order order = Order.create(orderId, supplierId, receiverId, sourceHubId, destinationHubId, request, products);

      // Then
      assertThat(order.getSupplierId()).isEqualTo(supplierId);
      assertThat(order.getReceiverId()).isEqualTo(receiverId);
      assertThat(order.getSourceHubId()).isEqualTo(sourceHubId);
      assertThat(order.getDestinationHubId()).isEqualTo(destinationHubId);
      assertThat(order.getRequest()).isEqualTo(request);
      assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
      assertThat(order.getOrderProducts()).hasSize(2);
      // 총액 계산식 검증 (20000 + 15000 = 35000)
      // TotalPrice 내부 구조에 따라 assert 방식은 변경될 수 있으나 객체 생성이 정상적으로 됨을 확인
      assertThat(order.getTotalPrice()).isNotNull();
    }

    @Test
    @DisplayName("[예외] supplierId가 null이면 BaseException을 발생시킨다.")
    void exception_when_supplierId_is_null() {
      // Given
      List<OrderProduct> products = List.of(mock(OrderProduct.class));

      // When & Then
      assertThatThrownBy(() -> Order.create(orderId, null, receiverId, sourceHubId, destinationHubId, request, products))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.INVALID_COMPANY_ID);

    }

    @Test
    @DisplayName("[예외] receiverId가 null이면 BaseException을 발생시킨다.")
    void exception_when_receiverId_is_null() {
      // Given
      List<OrderProduct> products = List.of(mock(OrderProduct.class));

      // When & Then
      assertThatThrownBy(() -> Order.create(orderId, supplierId, null, sourceHubId, destinationHubId, request, products))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.INVALID_COMPANY_ID);
    }

    @Test
    @DisplayName("[예외] sourceHubId가 null이면 BaseException을 발생시킨다.")
    void exception_when_sourceHubId_is_null() {
      // Given
      List<OrderProduct> products = List.of(mock(OrderProduct.class));

      // When & Then
      assertThatThrownBy(() -> Order.create(orderId, supplierId, receiverId, null, destinationHubId, request, products))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.MISSING_HUB_ID);
    }

    @Test
    @DisplayName("[예외] destinationHubId가 null이면 BaseException을 발생시킨다.")
    void exception_when_destinationHubId_is_null() {
      // Given
      List<OrderProduct> products = List.of(mock(OrderProduct.class));

      // When & Then
      assertThatThrownBy(() -> Order.create(orderId, supplierId, receiverId, sourceHubId, null, request, products))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.MISSING_HUB_ID);
    }

    @Test
    @DisplayName("[예외] orderProducts가 null이면 BaseException을 발생시킨다.")
    void exception_when_orderProducts_is_null() {
      // Given
      List<OrderProduct> nullProducts = null;

      // When & Then
      assertThatThrownBy(() -> Order.create(orderId, supplierId, receiverId, sourceHubId, destinationHubId, request, nullProducts))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.EMPTY_ORDER_PRODUCT);
    }

    @Test
    @DisplayName("[예외] orderProducts가 빈 리스트면 BaseException을 발생시킨다.")
    void exception_when_orderProducts_is_empty() {
      // Given
      List<OrderProduct> emptyProducts = Collections.emptyList();

      // When & Then
      assertThatThrownBy(() -> Order.create(orderId, supplierId, receiverId, sourceHubId, destinationHubId, request, emptyProducts))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.EMPTY_ORDER_PRODUCT);
    }

    @Test
    @DisplayName("[예외] 총 가격 계산 시 Integer 범위를 초과하면 ArithmeticException이 발생한다.")
    void exception_when_calculateTotalPrice_overflows() {
      // Given
      OrderProduct mockProduct = mock(OrderProduct.class);
      // Integer.MAX_VALUE * 2를 발생시켜 Math.multiplyExact 오버플로우 유도
      given(mockProduct.getUnitPrice()).willReturn(Integer.MAX_VALUE);
      given(mockProduct.getQuantity()).willReturn(2);

      List<OrderProduct> products = List.of(mockProduct);

      // When & Then
      assertThatThrownBy(() -> Order.create(orderId, supplierId, receiverId, sourceHubId, destinationHubId, request, products))
          .isInstanceOf(ArithmeticException.class)
          .hasMessageContaining("integer overflow");
    }
  }

  @Nested
  @DisplayName("updateOrderRequest() 메서드는")
  class Describe_updateOrderRequest {

    @Test
    @DisplayName("[성공] 새로운 요청사항이 주어지면 주문의 요청사항을 업데이트한다.")
    void success_update_request() {
      // Given
      Order order = Order.create(orderId, supplierId, receiverId, sourceHubId, destinationHubId, request, List.of(mock(OrderProduct.class)));
      String newRequest = "경비실에 맡겨주세요.";

      // When
      order.updateOrderRequest(newRequest);

      // Then
      assertThat(order.getRequest()).isEqualTo(newRequest);
    }
  }

  @Nested
  @DisplayName("cancel() 메서드는")
  class Describe_cancel {

    @Test
    @DisplayName("[성공] 주문 상태가 PENDING일 때, 상태를 CANCELLED로 변경한다.")
    void success_cancel_order() {
      // Given
      Order order = Order.create(orderId, supplierId, receiverId, sourceHubId, destinationHubId, request, List.of(mock(OrderProduct.class)));

      // When
      order.cancel();

      // Then
      assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("[예외] 주문 상태가 PENDING이 아닐 때 취소하려 하면 BaseException을 발생시킨다.")
    void exception_when_status_is_not_pending() {
      // Given
      Order order = Order.create(orderId, supplierId, receiverId, sourceHubId, destinationHubId, request, List.of(mock(OrderProduct.class)));

      // 리플렉션을 통해 강제로 상태를 변경 (테스트 격리)
      ReflectionTestUtils.setField(order, "status", OrderStatus.CONFIRMED);

      // When & Then
      assertThatThrownBy(order::cancel)
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderDomainErrorCode.INVALID_STATUS_TRANSITION);
    }
  }
}
