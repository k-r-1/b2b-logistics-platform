package com.boxoffice.companyservice.product.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductUpdateRequestDto {

    @Size(max = 255, message = "상품명은 255자를 초과할 수 없습니다.")
    private String name;

    @PositiveOrZero(message = "상품 가격은 0 이상이어야 합니다.")
    private Integer price;

    @PositiveOrZero(message = "상품 재고 수량은 0 이상이어야 합니다.")
    private Integer stockQuantity;

    public boolean hasUpdateField() {
        return name != null || price != null || stockQuantity != null;
    }

    public boolean hasBlankName() {
        return name != null && name.isBlank();
    }
}
