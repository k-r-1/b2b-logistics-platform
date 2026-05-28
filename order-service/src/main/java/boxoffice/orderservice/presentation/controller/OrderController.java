package boxoffice.orderservice.presentation.controller;

import boxoffice.orderservice.application.service.GetOrderService;
import boxoffice.orderservice.application.service.OrderCreateService;
import boxoffice.orderservice.application.service.dto.CreateOrderCommand;
import boxoffice.orderservice.application.service.dto.OrderResultDto;
import boxoffice.orderservice.presentation.dto.request.CreateOrderRequestDto;
import boxoffice.orderservice.presentation.dto.response.CreateOrderResponseDto;
import boxoffice.orderservice.presentation.dto.response.GetOrderResponseDto;
import com.boxoffice.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCreateService orderCreateService;
    private final GetOrderService getOrderService;

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
