package com.boxoffice.hubservice.hub.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record HubAssignManagerRequestDto(
        @NotNull UUID managerId
) { }

