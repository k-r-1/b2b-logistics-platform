package boxoffice.orderservice.presentation.controller;

import boxoffice.orderservice.application.service.command.DeleteOrderService;
import boxoffice.orderservice.application.service.query.GetOrderService;
import boxoffice.orderservice.application.service.command.OrderCreateService;
import boxoffice.orderservice.application.service.dto.CreateOrderCommand;
import boxoffice.orderservice.application.service.dto.OrderResultDto;
import boxoffice.orderservice.application.service.dto.SearchOrderFilter;
import boxoffice.orderservice.application.service.query.SearchOrdersService;
import boxoffice.orderservice.application.service.command.UpdateOrderService;
import boxoffice.orderservice.domain.enums.OrderStatus;
import boxoffice.orderservice.presentation.dto.request.CreateOrderRequestDto;
import boxoffice.orderservice.presentation.dto.request.UpdateOrderRequest;
import boxoffice.orderservice.presentation.dto.response.CreateOrderResponseDto;
import boxoffice.orderservice.presentation.dto.response.GetOrderResponseDto;
import com.boxoffice.common.response.ApiResponse;
import boxoffice.orderservice.presentation.dto.response.OrderSummaryResponse;
import com.boxoffice.common.response.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCreateService orderCreateService;
    private final GetOrderService getOrderService;
    private final UpdateOrderService updateOrderService;
    private final DeleteOrderService deleteOrderService;
    private final SearchOrdersService searchOrdersService;

    /**
     * 주문 목록 조회.
     *
     * 공통 필터: status, startDate/endDate 또는 yearMonth(월 단위 shortcut)
     * 허브 방향 필터(HUB_MANAGER/MASTER): sourceHubId, destinationHubId
     *   - HUB_MANAGER: 파라미터 제공 시 자신의 hubId로 방향 고정
     *   - MASTER: 파라미터 값이 실제 필터 UUID로 사용됨
     */
    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> searchOrders(
        @RequestHeader("X-User-Id") String keycloakId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) UUID sourceHubId,
        @RequestParam(required = false) UUID destinationHubId
    ) {
        LocalDate resolvedStart = yearMonth != null ? yearMonth.atDay(1) : startDate;
        LocalDate resolvedEnd = yearMonth != null ? yearMonth.atEndOfMonth() : endDate;

        SearchOrderFilter filter = new SearchOrderFilter(status, resolvedStart, resolvedEnd, sourceHubId, destinationHubId);
        Page<OrderSummaryResponse> result = searchOrdersService.searchOrders(keycloakId, page, size, filter);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponseDto>> createOrder(
        @RequestHeader("X-User-Id") String keycloakId,
        @Valid @RequestBody CreateOrderRequestDto requestDto
    ) {
        CreateOrderCommand command = toCommand(requestDto);
        OrderResultDto result = orderCreateService.createOrder(command, keycloakId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(HttpStatus.CREATED, CreateOrderResponseDto.from(result)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<GetOrderResponseDto>> getOrder(
        @RequestHeader("X-User-Id") String keycloakId,
        @PathVariable UUID orderId
    ) {
        OrderResultDto result = getOrderService.getOrder(orderId, keycloakId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, GetOrderResponseDto.from(result)));
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<CreateOrderResponseDto> updateOrder(
        @RequestHeader("X-User-Id") String keycloakId,
        @PathVariable UUID orderId,
        @RequestBody UpdateOrderRequest request
    ) {
        CreateOrderResponseDto response = updateOrderService.updateOrder(orderId, request, keycloakId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
        @RequestHeader("X-User-Id") String keycloakId,
        @PathVariable UUID orderId
    ) {
        deleteOrderService.deleteOrder(orderId, keycloakId);
        return ResponseEntity.noContent().build();
    }

    private CreateOrderCommand toCommand(CreateOrderRequestDto dto) {
        return new CreateOrderCommand(
            dto.supplierId(),
            dto.receiverId(),
            dto.request(),
            dto.products().stream()
                .map(p -> new CreateOrderCommand.ProductItem(p.productId(), p.quantity()))
                .toList(),
            new CreateOrderCommand.DeliveryAddress(
                dto.deliveryAddress().zipCode(),
                dto.deliveryAddress().address(),
                dto.deliveryAddress().detailAddress()
            ),
            dto.recipientName()
        );
    }
}
