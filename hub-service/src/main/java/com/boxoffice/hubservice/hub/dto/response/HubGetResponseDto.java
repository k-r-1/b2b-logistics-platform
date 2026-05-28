package com.boxoffice.hubservice.hub.dto.response;

import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;

import java.time.LocalDateTime;
import java.util.UUID;

public record HubGetResponseDto(
        UUID hubId,
        String name,
        String zipCode,
        String address,
        String detailAddress,
        Double latitude,
        Double longitude,
        HubType hubType,
        UUID managerId,
        Integer capacity,
        String closingReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HubGetResponseDto from(Hub hub) {
        return new HubGetResponseDto(
                hub.getId(),
                hub.getName(),
                hub.getAddress().getZipCode(),
                hub.getAddress().getAddress(),
                hub.getAddress().getDetailAddress(),
                hub.getCoordinate().getLatitude(),
                hub.getCoordinate().getLongitude(),
                hub.getHubType(),
                hub.getManagerId(),
                hub.getCapacity(),
                hub.getClosingReason(),
                hub.getCreatedAt(),
                hub.getUpdatedAt()
        );
    }
}