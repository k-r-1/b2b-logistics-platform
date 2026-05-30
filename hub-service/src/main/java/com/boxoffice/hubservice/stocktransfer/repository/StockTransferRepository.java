package com.boxoffice.hubservice.stocktransfer.repository;

import com.boxoffice.hubservice.stocktransfer.entity.StockTransfer;
import com.boxoffice.hubservice.stocktransfer.entity.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;
import java.util.UUID;

public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID>,
        QuerydslPredicateExecutor<StockTransfer> {

    boolean existsByFromHubIdAndStatusIn(UUID fromHubId, List<TransferStatus> statuses);

    long countByFromHubIdAndStatus(UUID fromHubId, TransferStatus status);
}
