package com.boxoffice.hubservice.hubroute.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record HubRouteCreateRequestDto(
        @NotNull UUID originHubId,
        @NotNull UUID destinationHubId,
        @NotNull @Positive Integer estimatedDurationMin,
        @NotNull @Positive Double estimatedDistanceKm
) {}
