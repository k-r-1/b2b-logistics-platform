package com.boxoffice.hubservice.stocktransfer.dto.response;

import java.util.UUID;

public record AssignedCompanyResponseDto(UUID companyId, String companyName, Integer stockCount) { }
