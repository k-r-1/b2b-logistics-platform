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
import boxoffice.orderservice.domain.vo.OrderSearchCondition;
import boxoffice.orderservice.domain.vo.TotalPrice;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import boxoffice.orderservice.presentation.dto.response.OrderSummaryResponse;
import com.boxoffice.common.exception.BaseException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchOrdersService 단위 테스트")
@ActiveProfiles("test")
class SearchOrdersServiceTest {

    @InjectMocks
    private SearchOrdersService searchOrdersService;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private OrderQueryService orderQueryService;

    private final String keycloakId = "user-abc";

    private Order buildMockOrder(UUID supplierId, UUID receiverId) {
        Order order = mock(Order.class);
        given(order.getId()).willReturn(UUID.randomUUID());
        given(order.getSupplierId()).willReturn(supplierId);
        given(order.getReceiverId()).willReturn(receiverId);
        given(order.getStatus()).willReturn(OrderStatus.PENDING);
        TotalPrice mockPrice = mock(TotalPrice.class);
        given(mockPrice.getValue()).willReturn(10000);
        given(order.getTotalPrice()).willReturn(mockPrice);
        given(order.getOrderProducts()).willReturn(List.of());
        return order;
    }

    @Nested
    @DisplayName("searchOrders() 메서드는")
    class Describe_searchOrders {

        @Test
        @DisplayName("[성공] MASTER는 조건 없이 전체 주문을 조회한다.")
        void searchOrders_성공_MASTER_전체조회() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(userFeignClient.getUserById(keycloakId)).willReturn(masterUser);

            Order order = buildMockOrder(UUID.randomUUID(), UUID.randomUUID());
            Page<Order> mockPage = new PageImpl<>(List.of(order));
            given(orderQueryService.searchOrders(any(OrderSearchCondition.class), any(Pageable.class)))
                .willReturn(mockPage);

            // when
            Page<OrderSummaryResponse> result = searchOrdersService.searchOrders(keycloakId, 0, 10);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            ArgumentCaptor<OrderSearchCondition> conditionCaptor = ArgumentCaptor.forClass(OrderSearchCondition.class);
            verify(orderQueryService).searchOrders(conditionCaptor.capture(), any(Pageable.class));
            assertThat(conditionCaptor.getValue().companyId()).isNull();
            assertThat(conditionCaptor.getValue().hubId()).isNull();
        }

        @Test
        @DisplayName("[성공] SUPPLIER_MANAGER는 companyId 조건(supplierId OR receiverId)으로 조회한다.")
        void searchOrders_성공_SUPPLIER_MANAGER_companyId조건() {
            // given
            UUID companyId = UUID.randomUUID();
            UserDetailInfo supplierUser = mock(UserDetailInfo.class);
            given(supplierUser.role()).willReturn("SUPPLIER_MANAGER");
            given(supplierUser.companyId()).willReturn(companyId);
            given(userFeignClient.getUserById(keycloakId)).willReturn(supplierUser);

            Order order = buildMockOrder(companyId, UUID.randomUUID());
            Page<Order> mockPage = new PageImpl<>(List.of(order));
            given(orderQueryService.searchOrders(any(OrderSearchCondition.class), any(Pageable.class)))
                .willReturn(mockPage);

            // when
            Page<OrderSummaryResponse> result = searchOrdersService.searchOrders(keycloakId, 0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);

            ArgumentCaptor<OrderSearchCondition> conditionCaptor = ArgumentCaptor.forClass(OrderSearchCondition.class);
            verify(orderQueryService).searchOrders(conditionCaptor.capture(), any(Pageable.class));
            assertThat(conditionCaptor.getValue().companyId()).isEqualTo(companyId);
            assertThat(conditionCaptor.getValue().hubId()).isNull();
        }

        @Test
        @DisplayName("[성공] HUB_MANAGER는 hubId 조건(sourceHubId OR destinationHubId)으로 조회한다.")
        void searchOrders_성공_HUB_MANAGER_hubId조건() {
            // given
            UUID hubId = UUID.randomUUID();
            UserDetailInfo hubUser = mock(UserDetailInfo.class);
            given(hubUser.role()).willReturn("HUB_MANAGER");
            given(hubUser.hubId()).willReturn(hubId);
            given(userFeignClient.getUserById(keycloakId)).willReturn(hubUser);

            Order order = buildMockOrder(UUID.randomUUID(), UUID.randomUUID());
            Page<Order> mockPage = new PageImpl<>(List.of(order));
            given(orderQueryService.searchOrders(any(OrderSearchCondition.class), any(Pageable.class)))
                .willReturn(mockPage);

            // when
            Page<OrderSummaryResponse> result = searchOrdersService.searchOrders(keycloakId, 0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);

            ArgumentCaptor<OrderSearchCondition> conditionCaptor = ArgumentCaptor.forClass(OrderSearchCondition.class);
            verify(orderQueryService).searchOrders(conditionCaptor.capture(), any(Pageable.class));
            assertThat(conditionCaptor.getValue().hubId()).isEqualTo(hubId);
            assertThat(conditionCaptor.getValue().companyId()).isNull();
        }

        @Test
        @DisplayName("[성공] DELIVERY_MANAGER는 HUB_MANAGER와 동일하게 hubId 조건으로 조회한다.")
        void searchOrders_성공_DELIVERY_MANAGER_hubId조건() {
            // given
            UUID hubId = UUID.randomUUID();
            UserDetailInfo deliveryUser = mock(UserDetailInfo.class);
            given(deliveryUser.role()).willReturn("DELIVERY_MANAGER");
            given(deliveryUser.hubId()).willReturn(hubId);
            given(userFeignClient.getUserById(keycloakId)).willReturn(deliveryUser);

            Page<Order> mockPage = new PageImpl<>(List.of());
            given(orderQueryService.searchOrders(any(OrderSearchCondition.class), any(Pageable.class)))
                .willReturn(mockPage);

            // when
            Page<OrderSummaryResponse> result = searchOrdersService.searchOrders(keycloakId, 0, 10);

            // then
            ArgumentCaptor<OrderSearchCondition> conditionCaptor = ArgumentCaptor.forClass(OrderSearchCondition.class);
            verify(orderQueryService).searchOrders(conditionCaptor.capture(), any(Pageable.class));
            assertThat(conditionCaptor.getValue().hubId()).isEqualTo(hubId);
        }

        @Test
        @DisplayName("[성공] 결과가 없을 경우 빈 Page를 반환한다.")
        void searchOrders_성공_빈결과() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(userFeignClient.getUserById(keycloakId)).willReturn(masterUser);

            given(orderQueryService.searchOrders(any(OrderSearchCondition.class), any(Pageable.class)))
                .willReturn(Page.empty());

            // when
            Page<OrderSummaryResponse> result = searchOrdersService.searchOrders(keycloakId, 0, 10);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("[성공] 허용되지 않은 size(예: 7)는 기본값 10으로 보정된다.")
        void searchOrders_성공_허용되지않은_size_보정() {
            // given
            UserDetailInfo masterUser = mock(UserDetailInfo.class);
            given(masterUser.role()).willReturn("MASTER");
            given(userFeignClient.getUserById(keycloakId)).willReturn(masterUser);

            given(orderQueryService.searchOrders(any(OrderSearchCondition.class), any(Pageable.class)))
                .willReturn(Page.empty());

            // when
            searchOrdersService.searchOrders(keycloakId, 0, 7);

            // then
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(orderQueryService).searchOrders(any(), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("[예외] 알 수 없는 역할은 UNAUTHORIZED_ORDER 예외를 발생시킨다.")
        void searchOrders_실패_알수없는_역할() {
            // given
            UserDetailInfo unknownUser = mock(UserDetailInfo.class);
            given(unknownUser.role()).willReturn("UNKNOWN_ROLE");
            given(userFeignClient.getUserById(keycloakId)).willReturn(unknownUser);

            // when & then
            assertThatThrownBy(() -> searchOrdersService.searchOrders(keycloakId, 0, 10))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);

            verify(orderQueryService, never()).searchOrders(any(), any());
        }

        @Test
        @DisplayName("[예외] SUPPLIER_MANAGER인데 companyId가 null이면 UNAUTHORIZED_ORDER 예외를 발생시킨다.")
        void searchOrders_실패_SUPPLIER_MANAGER_companyId_null() {
            // given
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("SUPPLIER_MANAGER");
            given(user.companyId()).willReturn(null);
            given(userFeignClient.getUserById(keycloakId)).willReturn(user);

            // when & then
            assertThatThrownBy(() -> searchOrdersService.searchOrders(keycloakId, 0, 10))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);

            verify(orderQueryService, never()).searchOrders(any(), any());
        }

        @Test
        @DisplayName("[예외] HUB_MANAGER인데 hubId가 null이면 UNAUTHORIZED_ORDER 예외를 발생시킨다.")
        void searchOrders_실패_HUB_MANAGER_hubId_null() {
            // given
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("HUB_MANAGER");
            given(user.hubId()).willReturn(null);
            given(userFeignClient.getUserById(keycloakId)).willReturn(user);

            // when & then
            assertThatThrownBy(() -> searchOrdersService.searchOrders(keycloakId, 0, 10))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);

            verify(orderQueryService, never()).searchOrders(any(), any());
        }
    }
}
