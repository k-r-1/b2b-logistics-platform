package com.boxoffice.hubservice.hubroute.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record HubRouteCreateRequestDto(
        @NotNull(message = "출발 허브 ID는 필수입니다.")
        UUID originHubId,

        @NotNull(message = "도착 허브 ID는 필수입니다.")
        UUID destinationHubId,

        @NotNull(message = "예상 소요 시간은 필수입니다.")
        @Positive(message = "예상 소요 시간은 양수여야 합니다.")
        Integer estimatedDurationMin,

        @NotNull(message = "예상 거리는 필수입니다.")
        @Positive(message = "예상 거리는 양수여야 합니다.")
        @DecimalMax(value = "999999.99", message = "예상 거리는 999999.99 이하여야 합니다.")
        Double estimatedDistanceKm
) { }
