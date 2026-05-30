package com.boxoffice.hubservice.client;

import java.util.UUID;

public record CompanyDetailResponseDto(UUID companyId, String companyName, Long stockCount) { }
