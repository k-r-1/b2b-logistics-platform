package boxoffice.orderservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import boxoffice.orderservice.application.client.UserFeignClient;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.domain.entity.Order;
import boxoffice.orderservice.domain.enums.OrderStatus;
import boxoffice.orderservice.domain.vo.TotalPrice;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import boxoffice.orderservice.presentation.dto.request.UpdateOrderRequest;
import boxoffice.orderservice.presentation.dto.response.CreateOrderResponseDto;
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
@DisplayName("UpdateOrderService 단위 테스트")
@ActiveProfiles("test")
class UpdateOrderServiceTest {

    @InjectMocks
    private UpdateOrderService updateOrderService;

    @Mock
    private UserFeignClient userFeignClient;
    @Mock
    private OrderQueryService orderQueryService;
    @Mock
    private OrderCommandService orderCommandService;

    private final String keycloakId = "user-abc";
    private final UUID orderId = UUID.randomUUID();
    private final UUID hubId = UUID.randomUUID();
    private final String updatedRequest = "문 앞에 놔주세요";

    private Order mockOrder;
    private UpdateOrderRequest updateRequest;

    @BeforeEach
    void setUp() {
        mockOrder = mock(Order.class);
        org.mockito.Mockito.lenient().when(mockOrder.getId()).thenReturn(orderId);
        org.mockito.Mockito.lenient().when(mockOrder.getSupplierId()).thenReturn(UUID.randomUUID());
        org.mockito.Mockito.lenient().when(mockOrder.getReceiverId()).thenReturn(UUID.randomUUID());
        org.mockito.Mockito.lenient().when(mockOrder.getRequest()).thenReturn(updatedRequest);
        org.mockito.Mockito.lenient().when(mockOrder.getStatus()).thenReturn(OrderStatus.PENDING);
        TotalPrice mockTotalPrice = mock(TotalPrice.class);
        org.mockito.Mockito.lenient().when(mockTotalPrice.getValue()).thenReturn(10000);
        org.mockito.Mockito.lenient().when(mockOrder.getTotalPrice()).thenReturn(mockTotalPrice);
        org.mockito.Mockito.lenient().when(mockOrder.getOrderProducts()).thenReturn(List.of());

        updateRequest = new UpdateOrderRequest(updatedRequest);
    }

    @Nested
    @DisplayName("updateOrder() 메서드는")
    class Describe_updateOrder {

        @Test
        @DisplayName("[성공] MASTER 권한 유저는 모든 주문을 수정할 수 있다.")
        void updateOrder_성공_MASTER() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(masterUser.isHubManager()).willReturn(false);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(masterUser));
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);
            given(orderCommandService.updateOrder(orderId, updatedRequest)).willReturn(mockOrder);

            // when
            CreateOrderResponseDto response = updateOrderService.updateOrder(orderId, updateRequest, keycloakId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.orderId()).isEqualTo(orderId);
            verify(orderCommandService).updateOrder(orderId, updatedRequest);
        }

        @Test
        @DisplayName("[성공] HUB_MANAGER 권한이며 sourceHubId가 본인 hubId와 일치하면 주문을 수정할 수 있다.")
        void updateOrder_성공_HUB_MANAGER_sourceHub_일치() {
            // given
            UserDetailInfo hubManagerUser = mock(UserDetailInfo.class);
            given(hubManagerUser.role()).willReturn("HUB_MANAGER");
            given(hubManagerUser.isHubManager()).willReturn(true);
            given(hubManagerUser.hubId()).willReturn(hubId);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(hubManagerUser));

            // sourceHubId 일치 시 단락 평가로 destinationHubId는 호출되지 않음
            given(mockOrder.getSourceHubId()).willReturn(hubId);
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);
            given(orderCommandService.updateOrder(orderId, updatedRequest)).willReturn(mockOrder);

            // when
            CreateOrderResponseDto response = updateOrderService.updateOrder(orderId, updateRequest, keycloakId);

            // then
            assertThat(response).isNotNull();
            verify(orderCommandService).updateOrder(orderId, updatedRequest);
        }

        @Test
        @DisplayName("[성공] HUB_MANAGER 권한이며 destinationHubId가 본인 hubId와 일치하면 주문을 수정할 수 있다.")
        void updateOrder_성공_HUB_MANAGER_destinationHub_일치() {
            // given
            UserDetailInfo hubManagerUser = mock(UserDetailInfo.class);
            given(hubManagerUser.role()).willReturn("HUB_MANAGER");
            given(hubManagerUser.isHubManager()).willReturn(true);
            given(hubManagerUser.hubId()).willReturn(hubId);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(hubManagerUser));

            given(mockOrder.getSourceHubId()).willReturn(UUID.randomUUID());
            given(mockOrder.getDestinationHubId()).willReturn(hubId);
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);
            given(orderCommandService.updateOrder(orderId, updatedRequest)).willReturn(mockOrder);

            // when
            CreateOrderResponseDto response = updateOrderService.updateOrder(orderId, updateRequest, keycloakId);

            // then
            assertThat(response).isNotNull();
            verify(orderCommandService).updateOrder(orderId, updatedRequest);
        }

        @Test
        @DisplayName("[예외] SUPPLIER_MANAGER 권한은 주문 수정이 불가하여 UNAUTHORIZED_ORDER 발생")
        void updateOrder_실패_SUPPLIER_MANAGER_권한없음() {
            // given
            UserDetailInfo supplierUser = mock(UserDetailInfo.class);
            given(supplierUser.role()).willReturn("SUPPLIER_MANAGER");
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(supplierUser));

            // when & then
            assertThatThrownBy(() -> updateOrderService.updateOrder(orderId, updateRequest, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);

            verify(orderQueryService, never()).findById(any());
            verify(orderCommandService, never()).updateOrder(any(), any());
        }

        @Test
        @DisplayName("[예외] DELIVERY_MANAGER 권한은 주문 수정이 불가하여 UNAUTHORIZED_ORDER 발생")
        void updateOrder_실패_DELIVERY_MANAGER_권한없음() {
            // given
            UserDetailInfo deliveryUser = mock(UserDetailInfo.class);
            given(deliveryUser.role()).willReturn("DELIVERY_MANAGER");
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(deliveryUser));

            // when & then
            assertThatThrownBy(() -> updateOrderService.updateOrder(orderId, updateRequest, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);

            verify(orderQueryService, never()).findById(any());
        }

        @Test
        @DisplayName("[예외] 알 수 없는 Role일 경우 UNAUTHORIZED_ORDER 발생")
        void updateOrder_실패_알수없는_권한() {
            // given
            UserDetailInfo unknownUser = mock(UserDetailInfo.class);
            given(unknownUser.role()).willReturn("UNKNOWN_ROLE");
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(unknownUser));

            // when & then
            assertThatThrownBy(() -> updateOrderService.updateOrder(orderId, updateRequest, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);
        }

        @Test
        @DisplayName("[예외] HUB_MANAGER 권한이지만 본인 허브와 관련 없는 주문이면 UNAUTHORIZED_HUB_ORDER 발생")
        void updateOrder_실패_HUB_MANAGER_허브_불일치() {
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
            assertThatThrownBy(() -> updateOrderService.updateOrder(orderId, updateRequest, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_HUB_ORDER);

            verify(orderCommandService, never()).updateOrder(any(), any());
        }

        @Test
        @DisplayName("[예외] 배송 완료(DELIVERED) 상태의 주문은 수정 불가하여 ORDER_ALREADY_DELIVERED 발생")
        void updateOrder_실패_배송완료_수정불가() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(masterUser.isHubManager()).willReturn(false);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(masterUser));

            given(mockOrder.getStatus()).willReturn(OrderStatus.DELIVERED);
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);

            // when & then
            assertThatThrownBy(() -> updateOrderService.updateOrder(orderId, updateRequest, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_ALREADY_DELIVERED);

            verify(orderCommandService, never()).updateOrder(any(), any());
        }

        @Test
        @DisplayName("[예외] 존재하지 않는 주문 ID로 요청 시 ORDER_NOT_FOUND 발생")
        void updateOrder_실패_주문없음() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(masterUser));

            given(orderQueryService.findById(orderId))
                .willThrow(new BaseException(OrderErrorCode.ORDER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> updateOrderService.updateOrder(orderId, updateRequest, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("[성공] request 필드가 null이면 기존 값을 유지한다.")
        void updateOrder_성공_null_필드_유지() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(masterUser.isHubManager()).willReturn(false);
            given(userFeignClient.getUserById(keycloakId)).willReturn(ApiResponse.success(masterUser));
            given(orderQueryService.findById(orderId)).willReturn(mockOrder);
            given(orderCommandService.updateOrder(orderId, null)).willReturn(mockOrder);

            UpdateOrderRequest nullRequest = new UpdateOrderRequest(null);

            // when
            CreateOrderResponseDto response = updateOrderService.updateOrder(orderId, nullRequest, keycloakId);

            // then
            assertThat(response).isNotNull();
            verify(orderCommandService).updateOrder(orderId, null);
        }
    }
}
