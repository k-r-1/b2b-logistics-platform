package com.boxoffice.hubservice.hub.dto.response;

import java.util.UUID;

public record HubActiveResponseDto(UUID hubId, boolean isActive) { }
