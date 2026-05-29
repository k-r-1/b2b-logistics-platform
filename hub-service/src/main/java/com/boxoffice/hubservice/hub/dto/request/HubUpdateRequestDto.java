package com.boxoffice.hubservice.hub.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record HubUpdateRequestDto(
        @Size(max = 100) String name,
        String zipCode,
        String address,
        String detailAddress,

        @DecimalMin("-90.0") @DecimalMax("90.0")
        Double latitude,

        @DecimalMin("-180.0") @DecimalMax("180.0")
        Double longitude,

        @Positive(message = "최대 수용량은 양수여야 합니다.")
        Integer capacity
) { }
