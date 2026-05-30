package com.boxoffice.hubservice.stocktransfer.event;

import java.util.UUID;

public record TransferDispatchedEvent(UUID transferId, UUID fromHubId, UUID toHubId) { }
