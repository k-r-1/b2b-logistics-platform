package com.boxoffice.companyservice.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ProductStockItemRequestDto {

    @NotNull(message = "상품 ID는 필수입니다.")
    private UUID productId;

    @NotNull(message = "상품 수량은 필수입니다.")
    @Min(value = 1, message = "상품 수량은 1 이상이어야 합니다.")
    private Integer quantity;
}
