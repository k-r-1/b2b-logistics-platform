package com.boxoffice.companyservice.product.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchCondition {

    private UUID companyId;
    private UUID hubId;
    private String name;
    private Integer minPrice;
    private Integer maxPrice;
    private Integer minStockQuantity;
    private Integer maxStockQuantity;
}
