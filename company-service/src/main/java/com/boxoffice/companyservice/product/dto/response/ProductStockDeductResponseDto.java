package com.boxoffice.companyservice.product.dto.response;

import com.boxoffice.companyservice.product.entity.Product;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductStockDeductResponseDto(
        UUID sourceHubId,
        UUID destinationHubId,
        List<ProductStockResult> details
) {

    public static ProductStockDeductResponseDto from(
            List<Product> products,
            Map<UUID, Integer> quantityByProductId,
            UUID sourceHubId,
            UUID destinationHubId
    ) {
        List<ProductStockResult> details = products.stream()
                .map(product -> ProductStockResult.from(product, quantityByProductId.get(product.getId())))
                .toList();

        return new ProductStockDeductResponseDto(sourceHubId, destinationHubId, details);
    }

    public record ProductStockResult(
            UUID productId,
            String productName,
            int unitPrice,
            int quantity
    ) {

        private static ProductStockResult from(Product product, Integer quantity) {
            return new ProductStockResult(
                    product.getId(),
                    product.getName(),
                    product.getPrice().getValue(),
                    quantity
            );
        }
    }
}
