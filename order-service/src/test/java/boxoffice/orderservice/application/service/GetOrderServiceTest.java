package boxoffice.orderservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import boxoffice.orderservice.application.client.UserInfoCacheService;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.application.service.dto.OrderResultDto;
import boxoffice.orderservice.application.service.query.GetOrderService;
import boxoffice.orderservice.application.service.query.OrderQueryService;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrderService 단위 테스트")
class GetOrderServiceTest {

    @InjectMocks
    private GetOrderService getOrderService;

    @Mock
    private UserInfoCacheService userInfoCacheService;

    @Mock
    private OrderQueryService orderQueryService;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final String keycloakId = "user-keycloak-123";
    private final UUID orderId = UUID.randomUUID();
    private final UUID supplierId = UUID.randomUUID();
    private final UUID receiverId = UUID.randomUUID();
    private final UUID sourceHubId = UUID.randomUUID();
    private final UUID destinationHubId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID hubId = UUID.randomUUID();

    private OrderResultDto orderResult;

    @BeforeEach
    void setUp() {
        orderResult = new OrderResultDto(
            orderId, supplierId, receiverId, sourceHubId, destinationHubId,
            "PENDING", 50_000, "조심히 와주세요", List.of()
        );
    }

    @Nested
    @DisplayName("getOrder() - MASTER 권한")
    class Describe_getOrder_master {

        @Test
        @DisplayName("[성공] MASTER는 모든 주문을 조회할 수 있다.")
        void success_master_can_access_any_order() {
            // given
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("MASTER");
            given(userInfoCacheService.getUserById(keycloakId)).willReturn(user);
            given(orderQueryService.findByIdAsDto(orderId)).willReturn(orderResult);

            // when
            OrderResultDto result = getOrderService.getOrder(orderId, keycloakId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.orderId()).isEqualTo(orderId);
            assertThat(result.status()).isEqualTo("PENDING");
        }
    }

    @Nested
    @DisplayName("getOrder() - COMPANY_MANAGER 권한")
    class Describe_getOrder_company_manager {

        @Test
        @DisplayName("[성공] COMPANY_MANAGER가 supplierId가 본인 companyId인 주문을 조회한다.")
        void success_company_manager_accesses_own_supplier_order() {
            // given
            OrderResultDto myOrder = new OrderResultDto(
                orderId, companyId, receiverId, sourceHubId, destinationHubId,
                "PENDING", 50_000, "요청사항", List.of()
            );
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("COMPANY_MANAGER");
            given(user.companyId()).willReturn(companyId);
            given(userInfoCacheService.getUserById(keycloakId)).willReturn(user);
            given(orderQueryService.findByIdAsDto(orderId)).willReturn(myOrder);

            // when
            OrderResultDto result = getOrderService.getOrder(orderId, keycloakId);

            // then
            assertThat(result.supplierId()).isEqualTo(companyId);
        }

        @Test
        @DisplayName("[성공] COMPANY_MANAGER가 receiverId가 본인 companyId인 주문을 조회한다.")
        void success_company_manager_accesses_own_receiver_order() {
            // given
            OrderResultDto myOrder = new OrderResultDto(
                orderId, supplierId, companyId, sourceHubId, destinationHubId,
                "PENDING", 50_000, "요청사항", List.of()
            );
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("COMPANY_MANAGER");
            given(user.companyId()).willReturn(companyId);
            given(userInfoCacheService.getUserById(keycloakId)).willReturn(user);
            given(orderQueryService.findByIdAsDto(orderId)).willReturn(myOrder);

            // when
            OrderResultDto result = getOrderService.getOrder(orderId, keycloakId);

            // then
            assertThat(result.receiverId()).isEqualTo(companyId);
        }

        @Test
        @DisplayName("[실패] COMPANY_MANAGER가 본인과 무관한 주문 조회 시 ORDER_NOT_FOUND 발생.")
        void failure_company_manager_accesses_other_order() {
            // given
            UUID otherId = UUID.randomUUID();
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("COMPANY_MANAGER");
            given(user.companyId()).willReturn(otherId);
            given(userInfoCacheService.getUserById(keycloakId)).willReturn(user);
            given(orderQueryService.findByIdAsDto(orderId)).willReturn(orderResult);

            // when & then
            assertThatThrownBy(() -> getOrderService.getOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getOrder() - HUB_MANAGER / DELIVERY_MANAGER 권한")
    class Describe_getOrder_hub_manager {

        @Test
        @DisplayName("[성공] HUB_MANAGER가 sourceHubId가 본인 hubId인 주문을 조회한다.")
        void success_hub_manager_accesses_source_hub_order() {
            // given
            OrderResultDto hubOrder = new OrderResultDto(
                orderId, supplierId, receiverId, hubId, destinationHubId,
                "PENDING", 50_000, "요청사항", List.of()
            );
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("HUB_MANAGER");
            given(user.hubId()).willReturn(hubId);
            given(userInfoCacheService.getUserById(keycloakId)).willReturn(user);
            given(orderQueryService.findByIdAsDto(orderId)).willReturn(hubOrder);

            // when
            OrderResultDto result = getOrderService.getOrder(orderId, keycloakId);

            // then
            assertThat(result.sourceHubId()).isEqualTo(hubId);
        }

        @Test
        @DisplayName("[성공] DELIVERY_MANAGER가 destinationHubId가 본인 hubId인 주문을 조회한다.")
        void success_delivery_manager_accesses_destination_hub_order() {
            // given
            OrderResultDto hubOrder = new OrderResultDto(
                orderId, supplierId, receiverId, sourceHubId, hubId,
                "PENDING", 50_000, "요청사항", List.of()
            );
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("DELIVERY_MANAGER");
            given(user.hubId()).willReturn(hubId);
            given(userInfoCacheService.getUserById(keycloakId)).willReturn(user);
            given(orderQueryService.findByIdAsDto(orderId)).willReturn(hubOrder);

            // when
            OrderResultDto result = getOrderService.getOrder(orderId, keycloakId);

            // then
            assertThat(result.destinationHubId()).isEqualTo(hubId);
        }

        @Test
        @DisplayName("[실패] HUB_MANAGER가 관할 외 허브 주문 조회 시 ORDER_NOT_FOUND 발생.")
        void failure_hub_manager_accesses_unrelated_order() {
            // given
            UUID otherHubId = UUID.randomUUID();
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("HUB_MANAGER");
            given(user.hubId()).willReturn(otherHubId);
            given(userInfoCacheService.getUserById(keycloakId)).willReturn(user);
            given(orderQueryService.findByIdAsDto(orderId)).willReturn(orderResult);

            // when & then
            assertThatThrownBy(() -> getOrderService.getOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getOrder() - 권한 미달 및 예외 시나리오")
    class Describe_getOrder_unauthorized_and_exception {

        @Test
        @DisplayName("[실패] 허용되지 않은 role일 경우 UNAUTHORIZED_ORDER 발생.")
        void failure_unknown_role_throws_unauthorized() {
            // given
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("UNKNOWN_ROLE");
            given(userInfoCacheService.getUserById(keycloakId)).willReturn(user);
            given(orderQueryService.findByIdAsDto(orderId)).willReturn(orderResult);

            // when & then
            assertThatThrownBy(() -> getOrderService.getOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 주문 조회 시 ORDER_NOT_FOUND 발생.")
        void failure_order_not_found() {
            // given
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("MASTER");
            given(userInfoCacheService.getUserById(keycloakId)).willReturn(user);
            given(orderQueryService.findByIdAsDto(orderId))
                .willThrow(new BaseException(OrderErrorCode.ORDER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> getOrderService.getOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("[예외] UserInfoCacheService Fallback 동작 시 USER_SERVICE_UNAVAILABLE 발생.")
        void exception_user_service_fallback() {
            // given
            given(userInfoCacheService.getUserById(keycloakId))
                .willThrow(new BaseException(OrderErrorCode.USER_SERVICE_UNAVAILABLE));

            // when & then
            assertThatThrownBy(() -> getOrderService.getOrder(orderId, keycloakId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.USER_SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("[예외] DB 타임아웃으로 OrderQueryService 실패 시 예외가 전파된다.")
        void exception_db_timeout_propagates() {
            // given
            UserDetailInfo user = mock(UserDetailInfo.class);
            given(user.role()).willReturn("MASTER");
            given(userInfoCacheService.getUserById(keycloakId)).willReturn(user);
            given(orderQueryService.findByIdAsDto(orderId))
                .willThrow(new RuntimeException("DB Timeout"));

            // when & then
            assertThatThrownBy(() -> getOrderService.getOrder(orderId, keycloakId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB Timeout");
        }
    }
}
