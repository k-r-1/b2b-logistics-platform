package boxoffice.orderservice.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequestDto(
    @NotNull UUID supplierId,
    @NotNull UUID receiverId,
    String request,
    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
    @Valid
    List<CreateProductRequest> products
) {
  public record CreateProductRequest(
      @NotNull(message = "상품 ID는 필수입니다.")
      UUID productId,

      @NotNull(message = "수량은 필수입니다.")
      @Positive(message = "수량은 1 이상이어야 합니다.")
      Integer quantity
  ) {}
}
