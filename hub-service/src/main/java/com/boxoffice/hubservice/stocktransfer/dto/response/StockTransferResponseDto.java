package com.boxoffice.hubservice.stocktransfer.dto.response;

import com.boxoffice.hubservice.stocktransfer.entity.StockTransfer;
import com.boxoffice.hubservice.stocktransfer.entity.TransferStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockTransferResponseDto(
        UUID transferId,
        UUID fromHubId,
        UUID toHubId,
        TransferStatus status,
        Integer totalProductCount,
        UUID managerId,
        LocalDateTime dispatchedAt,
        LocalDateTime completedAt,
        String note,
        UUID deliveryManagerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StockTransferResponseDto from(StockTransfer transfer) {
        return new StockTransferResponseDto(
                transfer.getId(),
                transfer.getFromHubId(),
                transfer.getToHubId(),
                transfer.getStatus(),
                transfer.getTotalProductCount(),
                transfer.getManagerId(),
                transfer.getDispatchedAt(),
                transfer.getCompletedAt(),
                transfer.getNote(),
                transfer.getDeliveryManagerId(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt()
        );
    }
}
