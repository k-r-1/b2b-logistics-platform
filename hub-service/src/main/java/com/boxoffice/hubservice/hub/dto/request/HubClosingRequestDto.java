package com.boxoffice.hubservice.hub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record HubClosingRequestDto(
        @NotBlank
        String reason
) { }
