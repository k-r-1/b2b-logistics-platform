package com.boxoffice.hubservice.hubroute.dto.response;

import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hubroute.entity.HubRoute;

import java.time.LocalDateTime;
import java.util.UUID;

public record HubRouteCreateResponseDto(
        UUID routeId,
        HubInfo originHub,
        HubInfo destinationHub,
        Integer estimatedDurationMin,
        Double estimatedDistanceKm,
        LocalDateTime createdAt,
        UUID createdBy
) {
    public record HubInfo(UUID hubId, String name, HubType hubType) {
        public static HubInfo from(Hub hub) {
            return new HubInfo(hub.getId(), hub.getName(), hub.getHubType());
        }
    }

    public static HubRouteCreateResponseDto from(HubRoute route, Hub originHub, Hub destinationHub) {
        return new HubRouteCreateResponseDto(
                route.getId(),
                HubInfo.from(originHub),
                HubInfo.from(destinationHub),
                route.getEstimatedDurationMin(),
                route.getEstimatedDistanceKm().doubleValue(),
                route.getCreatedAt(),
                route.getCreatedBy()
        );
    }
}
