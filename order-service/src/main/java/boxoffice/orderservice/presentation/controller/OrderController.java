package boxoffice.orderservice.presentation.controller;

import boxoffice.orderservice.application.service.GetOrderService;
import boxoffice.orderservice.application.service.OrderCreateService;
import boxoffice.orderservice.application.service.UpdateOrderService;
import boxoffice.orderservice.presentation.dto.request.CreateOrderRequestDto;
import boxoffice.orderservice.presentation.dto.request.UpdateOrderRequest;
import boxoffice.orderservice.presentation.dto.response.CreateOrderResponseDto;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
  private final OrderCreateService orderService;
  private final GetOrderService getOrderService;
  private final UpdateOrderService updateOrderService;

  @PostMapping
  public ResponseEntity<CreateOrderResponseDto> createOrder(
      @RequestHeader("X-User-Id") String keycloakId,
      @Valid @RequestBody CreateOrderRequestDto request
  ) {
    CreateOrderResponseDto response = orderService.createOrder(request, keycloakId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<CreateOrderResponseDto> getOrder(
      @RequestHeader("X-User-Id") String keycloakId,
      @PathVariable UUID orderId
  ) {
    CreateOrderResponseDto response = getOrderService.getOrder(orderId, keycloakId);
    return ResponseEntity.ok(response);
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

}
