package com.boxoffice.companyservice.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ProductStockDeductRequestDto {

    @NotNull(message = "공급 업체 ID는 필수입니다.")
    private UUID supplierId;

    @NotNull(message = "수령 업체 ID는 필수입니다.")
    private UUID receiverId;

    @Valid
    @NotEmpty(message = "상품 목록은 필수입니다.")
    private List<ProductStockItemRequestDto> products;
}
