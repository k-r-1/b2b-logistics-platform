package boxoffice.orderservice.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import boxoffice.orderservice.application.client.CompanyProductFeignClient;
import boxoffice.orderservice.application.client.UserFeignClient;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.application.service.command.DeleteOrderService;
import boxoffice.orderservice.application.service.command.OrderCommandService;
import boxoffice.orderservice.application.service.query.OrderQueryService;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.enums.OrderStatus;
import boxoffice.orderservice.domain.vo.TotalPrice;
import boxoffice.orderservice.infra.event.OrderCancelledEvent;
import boxoffice.orderservice.infra.event.OrderEventListener;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.ApiResponse;
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
@DisplayName("DeleteOrderService 단위 테스트")
@ActiveProfiles("test")
class DeleteOrderServiceTest {

    @InjectMocks
    private DeleteOrderService deleteOrderService;

    @Mock
    private UserFeignClient userFeignClient;
    @Mock
    private CompanyProductFeignClient companyProductFeignClient;
    @Mock
    private OrderQueryService orderQueryService;
    @Mock
    private OrderCommandService orderCommandService;
    @Mock
    private OrderEventListener orderEventListener;

    private final String keycloakId = "user-abc";
    private final UUID orderId = UUID.randomUUID();
    private final UUID hubId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private Order mockOrder;

    @BeforeEach
    void setUp() {
        mockOrder = mock(Order.class);
        org.mockito.Mockito.lenient().when(mockOrder.getId()).thenReturn(orderId);
        org.mockito.Mockito.lenient().when(mockOrder.getSupplierId()).thenReturn(UUID.randomUUID());
        org.mockito.Mockito.lenient().when(mockOrder.getReceiverId()).thenReturn(UUID.randomUUID());
        org.mockito.Mockito.lenient().when(mockOrder.getStatus()).thenReturn(OrderStatus.PENDING);
        org.mockito.Mockito.lenient().when(mockOrder.getOrderProducts()).thenReturn(List.of());
        TotalPrice mockTotalPrice = mock(TotalPrice.class);
        org.mockito.Mockito.lenient().when(mockTotalPrice.getValue()).thenReturn(10000);
        org.mockito.Mockito.lenient().when(mockOrder.getTotalPrice()).thenReturn(mockTotalPrice);
    }

    @Nested
    @DisplayName("deleteOrder() 메서드는")
    class Describe_deleteOrder {

        @Test
        @DisplayName("[성공] MASTER 권한은 모든 PENDING 주문을 취소할 수 있다.")
        void deleteOrder_성공_MASTER() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(masterUser.isHubManager()).willReturn(false);
            given(masterUser.userId()).willReturn(userId);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(masterUser));
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);
            given(orderCommandService.cancelOrder(mockOrder, userId)).willReturn(mockOrder);

            // when
            deleteOrderService.deleteOrder(orderId, keycloakId);

            // then
            verify(orderCommandService).cancelOrder(mockOrder, userId);
            verify(orderEventListener).orderCancelledEvent(new OrderCancelledEvent(orderId));
        }

        @Test
        @DisplayName("[성공] HUB_MANAGER 권한이며 sourceHubId가 본인 hubId와 일치하면 취소할 수 있다.")
        void deleteOrder_성공_HUB_MANAGER_sourceHub_일치() {
            // given
            UserDetailInfo hubManagerUser = mock(UserDetailInfo.class);
            given(hubManagerUser.role()).willReturn("HUB_MANAGER");
            given(hubManagerUser.isHubManager()).willReturn(true);
            given(hubManagerUser.hubId()).willReturn(hubId);
            given(hubManagerUser.userId()).willReturn(userId);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(hubManagerUser));

            given(mockOrder.getSourceHubId()).willReturn(hubId);
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);
            given(orderCommandService.cancelOrder(mockOrder, userId)).willReturn(mockOrder);

            // when
            deleteOrderService.deleteOrder(orderId, keycloakId);

            // then
            verify(orderCommandService).cancelOrder(mockOrder, userId);
            verify(orderEventListener).orderCancelledEvent(new OrderCancelledEvent(orderId));
        }

        @Test
        @DisplayName("[성공] HUB_MANAGER 권한이며 destinationHubId가 본인 hubId와 일치하면 취소할 수 있다.")
        void deleteOrder_성공_HUB_MANAGER_destinationHub_일치() {
            // given
            UserDetailInfo hubManagerUser = mock(UserDetailInfo.class);
            given(hubManagerUser.role()).willReturn("HUB_MANAGER");
            given(hubManagerUser.isHubManager()).willReturn(true);
            given(hubManagerUser.hubId()).willReturn(hubId);
            given(hubManagerUser.userId()).willReturn(userId);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(hubManagerUser));

            given(mockOrder.getSourceHubId()).willReturn(UUID.randomUUID());
            given(mockOrder.getDestinationHubId()).willReturn(hubId);
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);
            given(orderCommandService.cancelOrder(mockOrder, userId)).willReturn(mockOrder);

            // when
            deleteOrderService.deleteOrder(orderId, keycloakId);

            // then
            verify(orderCommandService).cancelOrder(mockOrder, userId);
        }

        @Test
        @DisplayName("[성공] 재고 복구 실패 시 예외를 삼키고 주문 취소는 정상 진행된다.")
        void deleteOrder_성공_재고복구_실패해도_취소진행() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(masterUser.isHubManager()).willReturn(false);
            given(masterUser.userId()).willReturn(userId);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(masterUser));
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);

            willThrow(new RuntimeException("재고 서비스 오류"))
                .given(companyProductFeignClient).restoreStocks(any(UUID.class), anyList());
            given(orderCommandService.cancelOrder(mockOrder, userId)).willReturn(mockOrder);

            // when
            deleteOrderService.deleteOrder(orderId, keycloakId);

            // then
            verify(orderCommandService).cancelOrder(mockOrder, userId);
            verify(orderEventListener).orderCancelledEvent(new OrderCancelledEvent(orderId));
        }

        @Test
        @DisplayName("[예외] SUPPLIER_MANAGER 권한은 주문 취소 불가 → UNAUTHORIZED_ORDER 발생")
        void deleteOrder_실패_SUPPLIER_MANAGER_권한없음() {
            // given
            UserDetailInfo supplierUser = mock(UserDetailInfo.class);
            given(supplierUser.role()).willReturn("SUPPLIER_MANAGER");
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(supplierUser));

            // when & then
            assertThatThrownBy(() -> deleteOrderService.deleteOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);

            verify(orderQueryService, never()).findById(any());
            verify(orderCommandService, never()).cancelOrder(any(), any());
        }

        @Test
        @DisplayName("[예외] DELIVERY_MANAGER 권한은 주문 취소 불가 → UNAUTHORIZED_ORDER 발생")
        void deleteOrder_실패_DELIVERY_MANAGER_권한없음() {
            // given
            UserDetailInfo deliveryUser = mock(UserDetailInfo.class);
            given(deliveryUser.role()).willReturn("DELIVERY_MANAGER");
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(deliveryUser));

            // when & then
            assertThatThrownBy(() -> deleteOrderService.deleteOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);

            verify(orderQueryService, never()).findById(any());
        }

        @Test
        @DisplayName("[예외] 알 수 없는 Role은 주문 취소 불가 → UNAUTHORIZED_ORDER 발생")
        void deleteOrder_실패_알수없는_권한() {
            // given
            UserDetailInfo unknownUser = mock(UserDetailInfo.class);
            given(unknownUser.role()).willReturn("UNKNOWN_ROLE");
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(unknownUser));

            // when & then
            assertThatThrownBy(() -> deleteOrderService.deleteOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);
        }

        @Test
        @DisplayName("[예외] HUB_MANAGER 권한이지만 본인 허브와 관련 없는 주문 → UNAUTHORIZED_HUB_ORDER 발생")
        void deleteOrder_실패_HUB_MANAGER_허브_불일치() {
            // given
            UserDetailInfo hubManagerUser = mock(UserDetailInfo.class);
            given(hubManagerUser.role()).willReturn("HUB_MANAGER");
            given(hubManagerUser.isHubManager()).willReturn(true);
            given(hubManagerUser.hubId()).willReturn(hubId);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(hubManagerUser));

            given(mockOrder.getSourceHubId()).willReturn(UUID.randomUUID());
            given(mockOrder.getDestinationHubId()).willReturn(UUID.randomUUID());
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);

            // when & then
            assertThatThrownBy(() -> deleteOrderService.deleteOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_HUB_ORDER);

            verify(orderCommandService, never()).cancelOrder(any(), any());
        }

        @Test
        @DisplayName("[예외] 존재하지 않는 주문 ID → ORDER_NOT_FOUND 발생")
        void deleteOrder_실패_주문없음() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(masterUser));

            given(orderQueryService.findById(orderId))
                .willThrow(new BaseException(OrderErrorCode.ORDER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> deleteOrderService.deleteOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);

            verify(orderCommandService, never()).cancelOrder(any(), any());
        }

        @Test
        @DisplayName("[예외] PENDING이 아닌 주문 취소 시도 → CANCEL_NOT_ALLOWED 발생")
        void deleteOrder_실패_PENDING_아닌_상태() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(masterUser.isHubManager()).willReturn(false);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(masterUser));

            given(mockOrder.getStatus()).willReturn(OrderStatus.CONFIRMED);
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);

            // when & then
            assertThatThrownBy(() -> deleteOrderService.deleteOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.CANCEL_NOT_ALLOWED);

            verify(orderCommandService, never()).cancelOrder(any(), any());
        }

        @Test
        @DisplayName("[예외] CANCELLED 상태의 주문 취소 시도 → CANCEL_NOT_ALLOWED 발생")
        void deleteOrder_실패_이미_취소된_주문() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(masterUser.isHubManager()).willReturn(false);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(masterUser));

            given(mockOrder.getStatus()).willReturn(OrderStatus.CANCELLED);
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);

            // when & then
            assertThatThrownBy(() -> deleteOrderService.deleteOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.CANCEL_NOT_ALLOWED);

            verify(orderCommandService, never()).cancelOrder(any(), any());
        }
    }
}
