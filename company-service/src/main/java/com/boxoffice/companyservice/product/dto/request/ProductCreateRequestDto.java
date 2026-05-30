package com.boxoffice.companyservice.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductCreateRequestDto {

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @NotNull(message = "상품 가격은 필수입니다.")
    @PositiveOrZero(message = "상품 가격은 0 이상이어야 합니다.")
    private Integer price;

    @NotNull(message = "상품 재고 수량은 필수입니다.")
    @PositiveOrZero(message = "상품 재고 수량은 0 이상이어야 합니다.")
    private Integer stockQuantity;
}
