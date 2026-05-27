package com.boxoffice.companyservice.product.dto.response;

import com.boxoffice.companyservice.product.entity.Product;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductStockCheckResponseDto(
        boolean isAllOrderable,
        List<StockDetail> details
) {

    public static ProductStockCheckResponseDto from(List<Product> products, Map<UUID, Integer> quantityByProductId) {
        List<StockDetail> details = products.stream()
                .map(product -> StockDetail.from(product, quantityByProductId.get(product.getId())))
                .toList();
        boolean isAllOrderable = details.stream().allMatch(StockDetail::isOrderable);

        return new ProductStockCheckResponseDto(isAllOrderable, details);
    }

    public record StockDetail(
            UUID productId,
            String productName,
            Integer unitPrice,
            boolean isOrderable
    ) {

        private static StockDetail from(Product product, Integer quantity) {
            return new StockDetail(
                    product.getId(),
                    product.getName(),
                    product.getPrice().getValue(),
                    product.getStockQuantity() >= quantity
            );
        }
    }
}
