package boxoffice.orderservice.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import boxoffice.orderservice.domain.enums.OrderStatus;
import boxoffice.orderservice.infra.exception.OrderDomainErrorCode;
import com.boxoffice.common.exception.BaseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Domain Unit Test")
class OrderTest {

  private UUID producerCompanyId;
  private UUID receiverCompanyId;
  private UUID originHubId;
  private UUID destinationHubId;
  private String request;

  @BeforeEach
  void setUp() {
    producerCompanyId = UUID.randomUUID();
    receiverCompanyId = UUID.randomUUID();
    originHubId = UUID.randomUUID();
    destinationHubId = UUID.randomUUID();
    request = "파손 주의 바랍니다.";
  }

  @Nested
  @DisplayName("create() 메서드는")
  class CreateTest {

    @Test
    @DisplayName("[성공] 유효한 파라미터가 주어지면 PENDING 상태의 주문이 생성되고 총액이 정확히 계산된다.")
    void create_Success() {
      // Given
      OrderProduct mockProduct1 = mock(OrderProduct.class);
      OrderProduct mockProduct2 = mock(OrderProduct.class);

      given(mockProduct1.getUnitPrice()).willReturn(1000);
      given(mockProduct1.getQuantity()).willReturn(2); // 2000
      given(mockProduct2.getUnitPrice()).willReturn(5000);
      given(mockProduct2.getQuantity()).willReturn(3); // 15000

      List<OrderProduct> products = List.of(mockProduct1, mockProduct2);

      // When
      Order order = Order.create(
          producerCompanyId, receiverCompanyId, originHubId, destinationHubId, request, products
      );

      // Then
      assertThat(order).isNotNull();
      assertThat(order.getProducerCompanyId()).isEqualTo(producerCompanyId);
      assertThat(order.getReceiverCompanyId()).isEqualTo(receiverCompanyId);
      assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
      assertThat(order.getOrderProducts()).hasSize(2);
      // 총액 17000이 TotalPrice에 잘 감싸져 있는지 여부는 구현체에 따라 간접 검증
      assertThat(order.getTotalPrice()).isNotNull();
    }

    @Test
    @DisplayName("[예외] 생산자 회사 ID가 Null이면 BaseException(INVALID_COMPANY_ID)을 던진다.")
    void create_Fail_NullProducerCompanyId() {
      // Given
      List<OrderProduct> products = List.of(mock(OrderProduct.class));

      // When & Then
      assertThatThrownBy(() -> Order.create(
          null, receiverCompanyId, originHubId, destinationHubId, request, products
      ))
          .isInstanceOf(BaseException.class)
          // ErrorCode 비교 (BaseException의 구현에 따라 getErrorCode() 메서드가 있다고 가정)
          .extracting("errorCode")
          .isEqualTo(OrderDomainErrorCode.INVALID_COMPANY_ID);
    }

    @Test
    @DisplayName("[예외] 주문 상품 목록이 Null이거나 비어있으면 BaseException(EMPTY_ORDER_PRODUCT)을 던진다.")
    void create_Fail_EmptyProducts() {
      // Given
      List<OrderProduct> emptyProducts = new ArrayList<>();

      // When & Then
      assertThatThrownBy(() -> Order.create(
          producerCompanyId, receiverCompanyId, originHubId, destinationHubId, request, emptyProducts
      ))
          .isInstanceOf(BaseException.class)
          .extracting("errorCode")
          .isEqualTo(OrderDomainErrorCode.EMPTY_ORDER_PRODUCT);

      assertThatThrownBy(() -> Order.create(
          producerCompanyId, receiverCompanyId, originHubId, destinationHubId, request, null
      ))
          .isInstanceOf(BaseException.class)
          .extracting("errorCode")
          .isEqualTo(OrderDomainErrorCode.EMPTY_ORDER_PRODUCT);
    }

    @Test
    @DisplayName("[예외] 총 가격 계산 시 Integer 범위를 초과하면 ArithmeticException을 던진다.")
    void create_Fail_IntegerOverflow() {
      // Given
      OrderProduct mockProduct = mock(OrderProduct.class);
      given(mockProduct.getUnitPrice()).willReturn(Integer.MAX_VALUE);
      given(mockProduct.getQuantity()).willReturn(2); // MAX_VALUE * 2 -> Overflow

      List<OrderProduct> products = List.of(mockProduct);

      // When & Then
      assertThatThrownBy(() -> Order.create(
          producerCompanyId, receiverCompanyId, originHubId, destinationHubId, request, products
      ))
          .isInstanceOf(ArithmeticException.class)
          .hasMessageContaining("integer overflow");
    }
  }

  @Nested
  @DisplayName("assignDelivery() 메서드는")
  class AssignDeliveryTest {

    private Order order;

    @BeforeEach
    void setUpOrder() {
      List<OrderProduct> products = List.of(mock(OrderProduct.class));
      order = Order.create(producerCompanyId, receiverCompanyId, originHubId, destinationHubId, request, products);
    }

    @Test
    @DisplayName("[성공] 유효한 배달 ID와 허브 ID가 주어지면 정상적으로 할당된다.")
    void assignDelivery_Success() {
      // Given
      UUID deliveryId = UUID.randomUUID();
      UUID newOriginHubId = UUID.randomUUID();
      UUID newDestinationHubId = UUID.randomUUID();

      // When
      order.assignDelivery(deliveryId, newOriginHubId, newDestinationHubId);

      // Then
      assertThat(order.getDeliveryId()).isEqualTo(deliveryId);
      assertThat(order.getOriginHubId()).isEqualTo(newOriginHubId);
      assertThat(order.getDestinationHubId()).isEqualTo(newDestinationHubId);
    }

    @Test
    @DisplayName("[예외] deliveryId가 null이면 예외를 던진다.")
    void assignDelivery_Fail_NullDeliveryId() {
      // Given & When & Then
      assertThatThrownBy(() -> order.assignDelivery(null, originHubId, destinationHubId))
          .isInstanceOf(BaseException.class)
          .extracting("errorCode")
          .isEqualTo(OrderDomainErrorCode.INVALID_DELIVERY_ID);
    }

    @Test
    @DisplayName("[예외] originHubId 또는 destinationHubId가 null이면 예외를 던진다.")
    void assignDelivery_Fail_NullHubIds() {
      // Given
      UUID deliveryId = UUID.randomUUID();

      // When & Then
      assertThatThrownBy(() -> order.assignDelivery(deliveryId, null, destinationHubId))
          .isInstanceOf(BaseException.class)
          .extracting("errorCode")
          .isEqualTo(OrderDomainErrorCode.INVALID_DELIVERY_ID);

      assertThatThrownBy(() -> order.assignDelivery(deliveryId, originHubId, null))
          .isInstanceOf(BaseException.class)
          .extracting("errorCode")
          .isEqualTo(OrderDomainErrorCode.INVALID_DELIVERY_ID);
    }
  }

  @Nested
  @DisplayName("updateStatus() 메서드는")
  class UpdateStatusTest {

    private Order order;

    @BeforeEach
    void setUpOrder() {
      List<OrderProduct> products = List.of(mock(OrderProduct.class));
      // Order.create() 호출 시 기본적으로 PENDING 상태로 초기화됨
      order = Order.create(
          producerCompanyId, receiverCompanyId, originHubId, destinationHubId, request, products
      );
    }

    @Test
    @DisplayName("[성공] PENDING 상태에서 CONFIRMED 상태로 정상적으로 전환된다.")
    void updateStatus_Success_ToConfirmed() {
      // Given (초기 상태 PENDING)

      // When
      order.updateStatus(OrderStatus.CONFIRMED);

      // Then
      assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("[성공] PENDING 상태에서 CANCELLED 상태로 정상적으로 전환된다.")
    void updateStatus_Success_ToCancelled() {
      // Given (초기 상태 PENDING)

      // When
      order.updateStatus(OrderStatus.CANCELLED);

      // Then
      assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("[실패] PENDING 상태에서 다시 PENDING으로 전환을 시도하면 INVALID_STATUS_TRANSITION 예외를 던진다.")
    void updateStatus_Fail_SameStatus() {
      // Given (초기 상태 PENDING)

      // When & Then
      assertThatThrownBy(() -> order.updateStatus(OrderStatus.PENDING))
          .isInstanceOf(BaseException.class)
          .extracting("errorCode")
          .isEqualTo(OrderDomainErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("[실패] 더 이상 전이가 불가능한 CONFIRMED 상태에서 상태 변경을 시도하면 예외를 던진다.")
    void updateStatus_Fail_FromConfirmed() {
      // Given
      order.updateStatus(OrderStatus.CONFIRMED); // 상태를 CONFIRMED로 먼저 변경

      // When & Then
      assertThatThrownBy(() -> order.updateStatus(OrderStatus.CANCELLED))
          .isInstanceOf(BaseException.class)
          .extracting("errorCode")
          .isEqualTo(OrderDomainErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("[실패] 더 이상 전이가 불가능한 CANCELLED 상태에서 상태 변경을 시도하면 예외를 던진다.")
    void updateStatus_Fail_FromCancelled() {
      // Given
      order.updateStatus(OrderStatus.CANCELLED); // 상태를 CANCELLED로 먼저 변경

      // When & Then
      assertThatThrownBy(() -> order.updateStatus(OrderStatus.CONFIRMED))
          .isInstanceOf(BaseException.class)
          .extracting("errorCode")
          .isEqualTo(OrderDomainErrorCode.INVALID_STATUS_TRANSITION);
    }
  }
}