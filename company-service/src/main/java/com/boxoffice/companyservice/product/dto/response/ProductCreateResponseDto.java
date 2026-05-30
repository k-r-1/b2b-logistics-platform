package com.boxoffice.companyservice.product.dto.response;

import com.boxoffice.companyservice.product.entity.Product;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// hubId는 Product 저장 필드가 아닌 Company 파생값이므로 생성 응답에서는 최소 필드만 반환한다.
public class ProductCreateResponseDto {

    private UUID productId;
    private UUID companyId;
    private String name;
    private Integer price;
    private Integer stockQuantity;

    private ProductCreateResponseDto(UUID productId, UUID companyId, String name, Integer price, Integer stockQuantity) {
        this.productId = productId;
        this.companyId = companyId;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public static ProductCreateResponseDto from(Product product) {
        return new ProductCreateResponseDto(
                product.getId(),
                product.getCompany().getId(),
                product.getName(),
                product.getPrice().getValue(),
                product.getStockQuantity()
        );
    }
}
