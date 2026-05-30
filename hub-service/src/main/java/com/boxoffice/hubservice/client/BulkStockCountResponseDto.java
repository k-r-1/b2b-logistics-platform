package com.boxoffice.hubservice.client;

import java.util.UUID;

public record BulkStockCountResponseDto(UUID hubId, Long stockCount) { }
