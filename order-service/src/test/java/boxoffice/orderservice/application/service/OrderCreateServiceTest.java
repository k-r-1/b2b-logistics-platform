package boxoffice.orderservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import boxoffice.orderservice.application.client.CompanyProductFeignClient;
import boxoffice.orderservice.application.client.UserFeignClient;
import boxoffice.orderservice.application.client.dto.UserDetailInfo;
import boxoffice.orderservice.application.client.dto.request.StockDeductRequest;
import boxoffice.orderservice.application.client.dto.response.InternalCompanyHub;
import boxoffice.orderservice.application.client.dto.response.StockDeductResponse;
import boxoffice.orderservice.application.service.dto.CreateOrderCommand;
import boxoffice.orderservice.application.service.dto.OrderResultDto;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreateService 단위 테스트")
class OrderCreateServiceTest {

    @InjectMocks
    private OrderCreateService orderCreateService;

    @Mock
    private UserFeignClient userFeignClient;
    @Mock
    private CompanyProductFeignClient companyProductFeignClient;
    @Mock
    private OrderCommandService orderCommandService;

    private final String requesterId = "user-123";
    private final UUID supplierId = UUID.randomUUID();
    private final UUID receiverId = UUID.randomUUID();
    private final UUID sourceHubId = UUID.randomUUID();
    private final UUID destinationHubId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private CreateOrderCommand command;
    private StockDeductResponse stockDeductResponse;
    private OrderResultDto orderResult;

    @BeforeEach
    void setUp() {
        command = new CreateOrderCommand(
            supplierId, receiverId, "조심히 와주세요",
            List.of(new CreateOrderCommand.ProductItem(productId, 10)),
            new CreateOrderCommand.DeliveryAddress("12345", "서울시 강남구 테헤란로 1", "101호"),
            "홍길동"
        );

        // record 타입은 mock 대신 실제 인스턴스 사용 (Mockito가 final record accessor를 스텁할 때
        // ArgumentMatcherStorage에 stale 상태가 생겨 이후 given() 호출에서 InvalidUseOfMatchersException 유발)
        stockDeductResponse = new StockDeductResponse(sourceHubId, destinationHubId, List.of());

        orderResult = new OrderResultDto(
            UUID.randomUUID(), supplierId, receiverId, sourceHubId, destinationHubId,
            "PENDING", 100_000, "조심히 와주세요", List.of()
        );
    }

    @Nested
    @DisplayName("createOrder() 메서드는")
    class Describe_createOrder {

        @Test
        @DisplayName("[성공] MASTER 권한으로 주문 생성 시, 재고 차감 후 저장하고 이벤트를 발행한다.")
        void success_with_master_role() {
            // given
            UserDetailInfo user = masterUser();
            given(userFeignClient.getUserById(requesterId)).willReturn(ApiResponse.success(user));
            given(companyProductFeignClient.deductStocks(any(UUID.class), any(StockDeductRequest.class)))
                .willReturn(ApiResponse.success(stockDeductResponse));
            given(orderCommandService.saveOrder(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(orderResult);

            // when
            OrderResultDto result = orderCreateService.createOrder(command, requesterId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("PENDING");
            verify(companyProductFeignClient).deductStocks(any(UUID.class), any(StockDeductRequest.class));
            verify(orderCommandService).saveOrder(any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("[성공] COMPANY_MANAGER는 receiverId가 본인 companyId로 강제 대체된다.")
        void success_company_manager_receiver_overridden() {
            // given
            UUID companyId = UUID.randomUUID();
            UserDetailInfo user = companyManagerUser(companyId);
            given(userFeignClient.getUserById(requesterId)).willReturn(ApiResponse.success(user));
            given(companyProductFeignClient.deductStocks(any(UUID.class), any(StockDeductRequest.class)))
                .willReturn(ApiResponse.success(stockDeductResponse));
            given(orderCommandService.saveOrder(any(), any(), any(UUID.class), any(), any(), any(), any(), any()))
                .willReturn(orderResult);

            // when
            OrderResultDto result = orderCreateService.createOrder(command, requesterId);

            // then
            assertThat(result).isNotNull();
            // saveOrder에 전달되는 receiverId가 companyId로 대체되었는지 검증
            verify(orderCommandService).saveOrder(
                any(), any(), Mockito.eq(companyId), any(), any(), any(), any(), any()
            );
        }

        @Test
        @DisplayName("[성공] HUB_MANAGER로 관할 허브 검증 통과 시 주문이 생성된다.")
        void success_with_hub_manager_role() {
            // given
            UserDetailInfo user = hubManagerUser(sourceHubId);
            given(userFeignClient.getUserById(requesterId)).willReturn(ApiResponse.success(user));

            // supplierHubId 매칭 시 short-circuit → receiverHubId는 미호출
            InternalCompanyHub hubs = new InternalCompanyHub(sourceHubId, destinationHubId);
            given(companyProductFeignClient.getCompanyById(supplierId, receiverId)).willReturn(ApiResponse.success(hubs));
            given(companyProductFeignClient.deductStocks(any(UUID.class), any(StockDeductRequest.class)))
                .willReturn(ApiResponse.success(stockDeductResponse));
            given(orderCommandService.saveOrder(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(orderResult);

            // when
            OrderResultDto result = orderCreateService.createOrder(command, requesterId);

            // then
            assertThat(result).isNotNull();
            verify(companyProductFeignClient).getCompanyById(supplierId, receiverId);
        }

        @Test
        @DisplayName("[예외] HUB_MANAGER가 관할 허브 외 주문 생성 시 UNAUTHORIZED_HUB_ORDER 발생")
        void exception_hub_manager_unauthorized_hub() {
            // given
            UserDetailInfo user = hubManagerUser(UUID.randomUUID()); // 다른 허브
            given(userFeignClient.getUserById(requesterId)).willReturn(ApiResponse.success(user));

            InternalCompanyHub hubs = new InternalCompanyHub(sourceHubId, destinationHubId);
            given(companyProductFeignClient.getCompanyById(supplierId, receiverId)).willReturn(ApiResponse.success(hubs));

            // when & then
            assertThatThrownBy(() -> orderCreateService.createOrder(command, requesterId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_HUB_ORDER);

            verify(companyProductFeignClient, never()).deductStocks(any(), any(StockDeductRequest.class));
        }

        @Test
        @DisplayName("[예외] 허용되지 않은 role일 경우 UNAUTHORIZED_ORDER 발생")
        void exception_unknown_role() {
            // given
            UserDetailInfo user = userWithRole("CUSTOMER");
            given(userFeignClient.getUserById(requesterId)).willReturn(ApiResponse.success(user));

            // when & then
            assertThatThrownBy(() -> orderCreateService.createOrder(command, requesterId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);
        }

        @Test
        @DisplayName("[예외] 유저 서비스 호출 실패(Fallback) 시 USER_SERVICE_UNAVAILABLE 발생")
        void exception_user_service_fallback() {
            // given
            given(userFeignClient.getUserById(requesterId))
                .willThrow(new BaseException(OrderErrorCode.USER_SERVICE_UNAVAILABLE));

            // when & then
            assertThatThrownBy(() -> orderCreateService.createOrder(command, requesterId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.USER_SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("[예외] 재고 차감 실패(Fallback) 시 STOCK_DEDUCT_FAILED 발생하고 주문이 저장되지 않는다.")
        void exception_stock_deduct_fallback() {
            // given
            UserDetailInfo user = masterUser();
            given(userFeignClient.getUserById(requesterId)).willReturn(ApiResponse.success(user));
            given(companyProductFeignClient.deductStocks(any(UUID.class), any(StockDeductRequest.class)))
                .willThrow(new BaseException(OrderErrorCode.STOCK_DEDUCT_FAILED));

            // when & then
            assertThatThrownBy(() -> orderCreateService.createOrder(command, requesterId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.STOCK_DEDUCT_FAILED);

            verify(orderCommandService, never()).saveOrder(any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("[예외] 주문 저장 실패 시 재고 보상 트랜잭션 수행 후 ORDER_SAVE_FAILED 발생")
        void exception_order_save_failed_triggers_compensation() {
            // given
            UserDetailInfo user = masterUser();
            given(userFeignClient.getUserById(requesterId)).willReturn(ApiResponse.success(user));
            given(companyProductFeignClient.deductStocks(any(UUID.class), any(StockDeductRequest.class)))
                .willReturn(ApiResponse.success(stockDeductResponse));
            // record 타입(StockDeductResponse, CreateOrderCommand)에 any(Class) 대신 any() 사용
            given(orderCommandService.saveOrder(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new RuntimeException("DB Timeout"));

            // when & then
            assertThatThrownBy(() -> orderCreateService.createOrder(command, requesterId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_SAVE_FAILED);

            verify(companyProductFeignClient).restoreStocks(any(UUID.class), anyList());
        }

        @Test
        @DisplayName("[예외] 보상 트랜잭션(재고 복구)도 실패해도 ORDER_SAVE_FAILED 예외는 정상 전파된다.")
        void exception_compensation_also_fails_still_propagates_original_error() {
            // given
            UserDetailInfo user = masterUser();
            given(userFeignClient.getUserById(requesterId)).willReturn(ApiResponse.success(user));
            given(companyProductFeignClient.deductStocks(any(UUID.class), any(StockDeductRequest.class)))
                .willReturn(ApiResponse.success(stockDeductResponse));
            given(orderCommandService.saveOrder(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new RuntimeException("DB Timeout"));
            doThrow(new RuntimeException("Feign Timeout")).when(companyProductFeignClient).restoreStocks(any(UUID.class), anyList());

            // when & then
            assertThatThrownBy(() -> orderCreateService.createOrder(command, requesterId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_SAVE_FAILED);
        }
    }



    // record 타입인 UserDetailInfo를 mock 대신 실제 인스턴스로 생성하는 헬퍼 메서드
    private UserDetailInfo masterUser() {
        return new UserDetailInfo(UUID.randomUUID(), "test@test.com", "tester", "MASTER", null, "ACTIVE", null);
    }

    private UserDetailInfo companyManagerUser(UUID companyId) {
        return new UserDetailInfo(UUID.randomUUID(), "test@test.com", "tester", "COMPANY_MANAGER", null, "ACTIVE", companyId);
    }

    private UserDetailInfo hubManagerUser(UUID hubId) {
        return new UserDetailInfo(UUID.randomUUID(), "test@test.com", "tester", "HUB_MANAGER", hubId, "ACTIVE", null);
    }

    private UserDetailInfo userWithRole(String role) {
        return new UserDetailInfo(UUID.randomUUID(), "test@test.com", "tester", role, null, "ACTIVE", null);
    }
}
