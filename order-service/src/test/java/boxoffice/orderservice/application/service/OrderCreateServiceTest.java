package boxoffice.orderservice.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import boxoffice.orderservice.application.client.CompanyProductFeignClient;
import boxoffice.orderservice.application.client.UserFeignClient;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.application.client.dto.response.InternalCompanyHub;
import boxoffice.orderservice.application.client.dto.response.StockDeductResponse;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.enums.OrderStatus;
import boxoffice.orderservice.domain.vo.TotalPrice;
import boxoffice.orderservice.infra.event.OrderCreatedEvent;
import boxoffice.orderservice.infra.event.OrderEventListener;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import boxoffice.orderservice.presentation.dto.request.CreateOrderRequestDto;
import boxoffice.orderservice.presentation.dto.response.CreateOrderResponseDto;
import com.boxoffice.common.exception.BaseException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreateService 단위 테스트")
@ActiveProfiles("test")
class OrderCreateServiceTest {

  @InjectMocks
  private OrderCreateService orderCreateService;

  @Mock
  private UserFeignClient userFeignClient;
  @Mock
  private CompanyProductFeignClient companyProductFeignClient;
  @Mock
  private OrderCommandService orderCommandService;
  @Mock
  private OrderEventListener eventPublisher;

  private final String requesterId = "user-123";
  private final UUID supplierId = UUID.randomUUID();
  private final UUID receiverId = UUID.randomUUID();
  private final String requestMsg = "조심히 와주세요";

  private CreateOrderRequestDto mockRequest;
  private CreateOrderRequestDto.CreateProductRequest mockProduct;

  @BeforeEach
  void setUp() {
    mockRequest = mock(CreateOrderRequestDto.class);
    mockProduct = mock(CreateOrderRequestDto.CreateProductRequest.class);

    // 공통 Request Mocking (Lenient하게 설정하여 불필요한 Stub 에러 방지)
    org.mockito.Mockito.lenient().when(mockRequest.supplierId()).thenReturn(supplierId);
    org.mockito.Mockito.lenient().when(mockRequest.receiverId()).thenReturn(receiverId);
    org.mockito.Mockito.lenient().when(mockRequest.request()).thenReturn(requestMsg);
    org.mockito.Mockito.lenient().when(mockRequest.products()).thenReturn(List.of(mockProduct));
    org.mockito.Mockito.lenient().when(mockProduct.productId()).thenReturn(UUID.randomUUID());
    org.mockito.Mockito.lenient().when(mockProduct.quantity()).thenReturn(10);

    CreateOrderRequestDto.AddressRequest mockAddress = mock(CreateOrderRequestDto.AddressRequest.class);
    org.mockito.Mockito.lenient().when(mockAddress.zipCode()).thenReturn("12345");
    org.mockito.Mockito.lenient().when(mockAddress.address()).thenReturn("서울시 강남구 테헤란로 1");
    org.mockito.Mockito.lenient().when(mockAddress.detailAddress()).thenReturn("101호");
    org.mockito.Mockito.lenient().when(mockRequest.deliveryAddress()).thenReturn(mockAddress);
    org.mockito.Mockito.lenient().when(mockRequest.recipientName()).thenReturn("홍길동");
    org.mockito.Mockito.lenient().when(mockRequest.recipientSlackId()).thenReturn("U12345678");
  }

  @Nested
  @DisplayName("createOrder() 메서드는")
  class Describe_createOrder {

    @Test
    @DisplayName("[성공] MASTER 권한 유저의 요청 시, 주문을 성공적으로 생성하고 이벤트를 발행한다.")
    void success_with_master_role() {
      // Given
      UserDetailInfo mockUser = mock(UserDetailInfo.class);
      given(mockUser.role()).willReturn("MASTER");
      given(userFeignClient.getUserById(requesterId)).willReturn(mockUser);

      StockDeductResponse mockDeductResponse = mock(StockDeductResponse.class);
      given(companyProductFeignClient.deductStocks(anyList())).willReturn(mockDeductResponse);

      Order mockOrder = mock(Order.class);
      given(mockOrder.getId()).willReturn(UUID.randomUUID());
      given(mockOrder.getSupplierId()).willReturn(supplierId);
      given(mockOrder.getReceiverId()).willReturn(receiverId);
      given(mockOrder.getSourceHubId()).willReturn(UUID.randomUUID());
      given(mockOrder.getDestinationHubId()).willReturn(UUID.randomUUID());
      given(mockOrder.getRequest()).willReturn(requestMsg);
      given(mockOrder.getStatus()).willReturn(OrderStatus.PENDING);

      TotalPrice mockTotalPrice = mock(TotalPrice.class);
      given(mockTotalPrice.getValue()).willReturn(30000);
      given(mockOrder.getTotalPrice()).willReturn(mockTotalPrice);

      given(orderCommandService.saveOrder(any(), any(), any(), any(), any(), any())).willReturn(mockOrder);

      // When
      CreateOrderResponseDto response = orderCreateService.createOrder(mockRequest, requesterId);

      // Then
      assertThat(response).isNotNull();

      // 검증(Verify) 시에도 명확하게 호출 여부를 확인합니다.
      verify(orderCommandService).saveOrder(any(), any(), any(), any(), any(), any());
      verify(eventPublisher).orderCreateEvent(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("[성공] HUB_MANAGER 권한이며 허브 검증을 통과하면 주문을 성공적으로 생성한다.")
    void success_with_hub_manager_role() {
      // Given
      UserDetailInfo mockUser = mock(UserDetailInfo.class);
      given(mockUser.role()).willReturn("HUB_MANAGER");
      given(mockUser.hubId()).willReturn(receiverId);
      given(userFeignClient.getUserById(requesterId)).willReturn(mockUser);

      InternalCompanyHub mockHub = mock(InternalCompanyHub.class);
      given(mockHub.receiverHubId()).willReturn(receiverId);
      given(companyProductFeignClient.getCompanyById(supplierId, receiverId)).willReturn(mockHub);

      StockDeductResponse mockDeductResponse = mock(StockDeductResponse.class);
      given(companyProductFeignClient.deductStocks(anyList())).willReturn(mockDeductResponse);

      // 1. buildEvent() 및 toResponse() 내부에서 호출되는 모든 Getter가 값을 반환하도록 가짜 Order 객체를 세팅합니다.
      Order mockOrder = mock(Order.class);
      given(mockOrder.getId()).willReturn(UUID.randomUUID());
      given(mockOrder.getSupplierId()).willReturn(supplierId);
      given(mockOrder.getReceiverId()).willReturn(receiverId);
      given(mockOrder.getSourceHubId()).willReturn(UUID.randomUUID());
      given(mockOrder.getDestinationHubId()).willReturn(UUID.randomUUID());
      given(mockOrder.getRequest()).willReturn(requestMsg);
      given(mockOrder.getStatus()).willReturn(OrderStatus.PENDING);

      TotalPrice mockTotalPrice = mock(TotalPrice.class);
      given(mockTotalPrice.getValue()).willReturn(30000);
      given(mockOrder.getTotalPrice()).willReturn(mockTotalPrice);

      // 2. 어떤 파라미터가 들어오더라도 위에서 정의한 mockOrder가 반환되도록 given을 설정합니다.
      // (기존의 mock(Order.class)를 그대로 넘겼거나 파라미터가 불일치하여 null이 반환되던 문제를 해결합니다.)
      given(orderCommandService.saveOrder(any(), any(), any(), any(), any(), any())).willReturn(mockOrder);

      // When
      CreateOrderResponseDto response = orderCreateService.createOrder(mockRequest, requesterId);

      // Then
      assertThat(response).isNotNull();
      verify(orderCommandService).saveOrder(any(), any(), any(), any(), any(), any());
      verify(eventPublisher).orderCreateEvent(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("[예외] HUB_MANAGER 권한이지만 허브 검증에 실패하면 UNAUTHORIZED_HUB_ORDER 발생")
    void exception_when_hub_manager_unauthorized() {
      // Given
      UserDetailInfo mockUser = mock(UserDetailInfo.class);
      given(mockUser.role()).willReturn("HUB_MANAGER");
      given(mockUser.hubId()).willReturn(UUID.randomUUID()); // receiverId와 불일치 유도
      given(userFeignClient.getUserById(requesterId)).willReturn(mockUser);

      InternalCompanyHub mockHub = mock(InternalCompanyHub.class);
      given(mockHub.receiverHubId()).willReturn(UUID.randomUUID());
      given(companyProductFeignClient.getCompanyById(supplierId, receiverId)).willReturn(mockHub);

      // When & Then
      assertThatThrownBy(() -> orderCreateService.createOrder(mockRequest, requesterId))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_HUB_ORDER);
    }

    @Test
    @DisplayName("[예외] 알 수 없는 Role(권한)일 경우 UNAUTHORIZED_ORDER 발생")
    void exception_when_role_unauthorized() {
      // Given
      UserDetailInfo mockUser = mock(UserDetailInfo.class);
      given(mockUser.role()).willReturn("CUSTOMER");
      given(userFeignClient.getUserById(requesterId)).willReturn(mockUser);

      // When & Then
      assertThatThrownBy(() -> orderCreateService.createOrder(mockRequest, requesterId))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);
    }

    @Test
    @DisplayName("[예외] 주문 저장 실패 시, 재고 복구(fallback)를 수행하고 ORDER_SAVE_FAILED 발생")
    void exception_when_save_order_fails_triggers_fallback() {
      // Given
      UserDetailInfo mockUser = mock(UserDetailInfo.class);
      given(mockUser.role()).willReturn("MASTER");
      given(userFeignClient.getUserById(requesterId)).willReturn(mockUser);

      given(companyProductFeignClient.deductStocks(anyList())).willReturn(mock(StockDeductResponse.class));

      // 주문 저장 시 강제로 RuntimeException 발생
      given(orderCommandService.saveOrder(any(), any(), any(), any(), any(), any()))
          .willThrow(new RuntimeException("DB Timeout"));

      // When & Then
      assertThatThrownBy(() -> orderCreateService.createOrder(mockRequest, requesterId))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_SAVE_FAILED);

      // 보상 트랜잭션인 restoreStocks API가 호출되었는지 검증
      verify(companyProductFeignClient).restoreStocks(anyList());
    }

    @Test
    @DisplayName("[예외] 보상 트랜잭션(fallback) 중 에러가 발생해도, ORDER_SAVE_FAILED 예외를 정상적으로 던진다.")
    void exception_when_fallback_also_fails() {
      // Given
      UserDetailInfo mockUser = mock(UserDetailInfo.class);
      given(mockUser.role()).willReturn("MASTER");
      given(userFeignClient.getUserById(requesterId)).willReturn(mockUser);

      given(companyProductFeignClient.deductStocks(anyList())).willReturn(mock(StockDeductResponse.class));

      given(orderCommandService.saveOrder(any(), any(), any(), any(), any(), any()))
          .willThrow(new RuntimeException("DB Timeout"));

      // 보상 트랜잭션(재고 복구)조차 실패하는 상황 가정
      doThrow(new RuntimeException("Feign Timeout")).when(companyProductFeignClient).restoreStocks(anyList());

      // When & Then
      assertThatThrownBy(() -> orderCreateService.createOrder(mockRequest, requesterId))
          .isInstanceOf(BaseException.class)
          .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_SAVE_FAILED);
    }
  }
}