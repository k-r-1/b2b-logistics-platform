package com.boxoffice.hubservice.client;

import java.util.List;
import java.util.UUID;

public record BulkStockCountRequestDto(List<UUID> hubIds) { }
