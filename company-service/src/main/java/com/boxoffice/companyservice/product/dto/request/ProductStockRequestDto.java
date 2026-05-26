package com.boxoffice.companyservice.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProductStockRequestDto {

    @Valid
    @NotEmpty(message = "상품 목록은 필수입니다.")
    private List<ProductStockItemRequestDto> items;
}
