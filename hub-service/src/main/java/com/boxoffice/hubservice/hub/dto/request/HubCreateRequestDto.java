package com.boxoffice.hubservice.hub.dto.request;

import com.boxoffice.hubservice.hub.entity.HubType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record HubCreateRequestDto(
        @NotBlank(message = "허브 이름은 필수입니다.")
        String name,

        String zipCode,

        @NotBlank(message = "주소는 필수입니다.")
        String address,

        String detailAddress,

        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0",  message = "위도는 90 이하이어야 합니다.")
        Double latitude,

        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0",  message = "경도는 180 이하이어야 합니다.")
        Double longitude,

        @NotNull(message = "허브 타입은 필수입니다.")
        HubType hubType,

        @Positive(message = "최대 수용량은 양수여야 합니다.")
        Integer capacity
) { }
