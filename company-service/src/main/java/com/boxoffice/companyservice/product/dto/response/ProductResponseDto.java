package com.boxoffice.companyservice.product.dto.response;

import com.boxoffice.companyservice.product.entity.Product;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductResponseDto {

    private UUID productId;
    private UUID companyId;
    private UUID hubId;
    private String name;
    private Integer price;
    private Integer stockQuantity;

    private ProductResponseDto(
            UUID productId,
            UUID companyId,
            UUID hubId,
            String name,
            Integer price,
            Integer stockQuantity
    ) {
        this.productId = productId;
        this.companyId = companyId;
        this.hubId = hubId;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public static ProductResponseDto from(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getCompany().getId(),
                product.getCompany().getHubId(),
                product.getName(),
                product.getPrice().getValue(),
                product.getStockQuantity()
        );
    }
}
