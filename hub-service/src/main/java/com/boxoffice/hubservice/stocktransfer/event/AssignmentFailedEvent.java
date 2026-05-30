package com.boxoffice.hubservice.stocktransfer.event;

import java.util.UUID;

public record AssignmentFailedEvent(UUID transferId, String reason) { }
