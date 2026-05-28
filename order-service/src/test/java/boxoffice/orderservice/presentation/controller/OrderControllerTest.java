package boxoffice.orderservice.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import boxoffice.orderservice.application.service.OrderCreateService;
import boxoffice.orderservice.application.service.dto.CreateOrderCommand;
import boxoffice.orderservice.application.service.dto.OrderResultDto;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import boxoffice.orderservice.presentation.dto.request.CreateOrderRequestDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController 단위 테스트")
class OrderControllerTest {

    @InjectMocks
    private OrderController orderController;

    @Mock
    private OrderCreateService orderCreateService;

    private final String keycloakId = "user-keycloak-123";
    private final UUID supplierId = UUID.randomUUID();
    private final UUID receiverId = UUID.randomUUID();
    private final UUID sourceHubId = UUID.randomUUID();
    private final UUID destinationHubId = UUID.randomUUID();

    private CreateOrderRequestDto requestDto;
    private OrderResultDto orderResult;

    @BeforeEach
    void setUp() {
        requestDto = new CreateOrderRequestDto(
            supplierId, receiverId, "조심히 와주세요",
            List.of(new CreateOrderRequestDto.CreateProductRequest(UUID.randomUUID(), 5)),
            new CreateOrderRequestDto.AddressRequest("12345", "서울시 강남구 테헤란로 1", "101호"),
            "홍길동", "U12345678"
        );

        orderResult = new OrderResultDto(
            UUID.randomUUID(), supplierId, receiverId, sourceHubId, destinationHubId,
            "PENDING", 50_000, "조심히 와주세요", List.of()
        );
    }

    @Nested
    @DisplayName("createOrder() 엔드포인트는")
    class Describe_createOrder {

        @Test
        @DisplayName("[성공] 유효한 요청 시 201 CREATED와 주문 응답을 반환한다.")
        void success_returns_201_created() {
            // given
            given(orderCreateService.createOrder(any(CreateOrderCommand.class), anyString()))
                .willReturn(orderResult);

            // when
            ResponseEntity<ApiResponse<CreateOrderResponseDto>> response = orderController.createOrder(keycloakId, requestDto);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData().orderId()).isEqualTo(orderResult.orderId());
            assertThat(response.getBody().getData().status()).isEqualTo("PENDING");
            assertThat(response.getBody().getMessage()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("[성공] RequestDto → Command 변환이 올바르게 수행된다.")
        void success_request_dto_to_command_conversion() {
            // given
            given(orderCreateService.createOrder(any(CreateOrderCommand.class), anyString()))
                .willReturn(orderResult);

            // when
            orderController.createOrder(keycloakId, requestDto);

            // then
            verify(orderCreateService).createOrder(
                new CreateOrderCommand(
                    supplierId, receiverId, "조심히 와주세요",
                    requestDto.products().stream()
                        .map(p -> new CreateOrderCommand.ProductItem(p.productId(), p.quantity()))
                        .toList(),
                    new CreateOrderCommand.DeliveryAddress("12345", "서울시 강남구 테헤란로 1", "101호"),
                    "홍길동"
                ),
                keycloakId
            );
        }

        @Test
        @DisplayName("[실패] 서비스에서 UNAUTHORIZED_ORDER 발생 시 예외가 그대로 전파된다.")
        void failure_unauthorized_propagates_exception() {
            // given
            given(orderCreateService.createOrder(any(CreateOrderCommand.class), anyString()))
                .willThrow(new BaseException(OrderErrorCode.UNAUTHORIZED_ORDER));

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> orderController.createOrder(keycloakId, requestDto)
            )
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.UNAUTHORIZED_ORDER);
        }

        @Test
        @DisplayName("[실패] 서비스에서 ORDER_SAVE_FAILED 발생 시 예외가 그대로 전파된다.")
        void failure_order_save_failed_propagates_exception() {
            // given
            given(orderCreateService.createOrder(any(CreateOrderCommand.class), anyString()))
                .willThrow(new BaseException(OrderErrorCode.ORDER_SAVE_FAILED));

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> orderController.createOrder(keycloakId, requestDto)
            )
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_SAVE_FAILED);
        }

        @Test
        @DisplayName("[예외] 배송 서비스 장애(Fallback) 시 DELIVERY_REQUEST_FAILED 예외가 전파된다.")
        void exception_delivery_service_fallback_propagates() {
            // given
            given(orderCreateService.createOrder(any(CreateOrderCommand.class), anyString()))
                .willThrow(new BaseException(OrderErrorCode.DELIVERY_REQUEST_FAILED));

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> orderController.createOrder(keycloakId, requestDto)
            )
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.DELIVERY_REQUEST_FAILED);
        }
    }
}
