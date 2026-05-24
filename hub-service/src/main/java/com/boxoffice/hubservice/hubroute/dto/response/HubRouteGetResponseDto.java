package com.boxoffice.hubservice.hubroute.dto.response;

import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hubroute.entity.HubRoute;

import java.time.LocalDateTime;
import java.util.UUID;

public record HubRouteGetResponseDto(
        UUID routeId,
        HubInfo originHub,
        HubInfo destinationHub,
        Integer estimatedDurationMin,
        Double estimatedDistanceKm,
        LocalDateTime createdAt,
        UUID createdBy,
        LocalDateTime updatedAt,
        UUID updatedBy
) {
    public record HubInfo(UUID hubId, String name, HubType hubType) {
        public static HubInfo from(Hub hub) {
            return new HubInfo(hub.getId(), hub.getName(), hub.getHubType());
        }
    }

    public static HubRouteGetResponseDto from(HubRoute route, Hub originHub, Hub destinationHub) {
        return new HubRouteGetResponseDto(
                route.getId(),
                HubInfo.from(originHub),
                HubInfo.from(destinationHub),
                route.getEstimatedDurationMin(),
                route.getEstimatedDistanceKm().doubleValue(),
                route.getCreatedAt(),
                route.getCreatedBy(),
                route.getUpdatedAt(),
                route.getUpdatedBy()
        );
    }
}