package boxoffice.orderservice.presentation.controller;

import boxoffice.orderservice.application.service.OrderCreateService;
import boxoffice.orderservice.presentation.dto.request.CreateOrderRequestDto;
import boxoffice.orderservice.presentation.dto.response.CreateOrderResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  @PostMapping
  public ResponseEntity<CreateOrderResponseDto> createOrder(
      @RequestHeader("X-User-Id") String keycloakId,
      @Valid @RequestBody CreateOrderRequestDto request
  ) {
    CreateOrderResponseDto response = orderService.createOrder(request, keycloakId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

}
