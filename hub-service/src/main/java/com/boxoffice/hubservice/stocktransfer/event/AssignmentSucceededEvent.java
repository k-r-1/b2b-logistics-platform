package com.boxoffice.hubservice.stocktransfer.event;

import java.util.UUID;

public record AssignmentSucceededEvent(UUID transferId, UUID deliveryManagerId) { }
