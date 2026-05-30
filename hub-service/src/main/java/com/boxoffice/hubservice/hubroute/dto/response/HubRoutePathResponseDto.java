package com.boxoffice.hubservice.hubroute.dto.response;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hubroute.entity.HubRoute;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public record HubRoutePathResponseDto(
        HubInfo originHub,
        HubInfo destinationHub,
        List<HubRouteSegmentDto> segments,
        int totalDurationMin,
        BigDecimal totalDistanceKm
) {
    public record HubRouteSegmentDto(
            int sequence,
            HubInfo originHub,
            HubInfo destinationHub,
            int estimatedDurationMin,
            BigDecimal estimatedDistanceKm
    ) { }

    public record HubInfo(UUID hubId, String name, HubType hubType) { }

    public static HubRoutePathResponseDto of(List<HubRoute> routes, Map<UUID, Hub> hubMap) {
        if (routes == null || routes.isEmpty()) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
        AtomicInteger seq = new AtomicInteger(1);
        List<HubRouteSegmentDto> segments = routes.stream()
                .map(r -> {
                    Hub origin = hubMap.get(r.getOriginHubId());
                    Hub dest = hubMap.get(r.getDestinationHubId());
                    return new HubRouteSegmentDto(
                            seq.getAndIncrement(),
                            new HubInfo(origin.getId(), origin.getName(), origin.getHubType()),
                            new HubInfo(dest.getId(), dest.getName(), dest.getHubType()),
                            r.getEstimatedDurationMin(),
                            r.getEstimatedDistanceKm()
                    );
                }).toList();

        int totalDuration = routes.stream().mapToInt(HubRoute::getEstimatedDurationMin).sum();
        BigDecimal totalDistance = routes.stream()
                .map(HubRoute::getEstimatedDistanceKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Hub first = hubMap.get(routes.get(0).getOriginHubId());
        Hub last = hubMap.get(routes.get(routes.size() - 1).getDestinationHubId());

        return new HubRoutePathResponseDto(
                new HubInfo(first.getId(), first.getName(), first.getHubType()),
                new HubInfo(last.getId(), last.getName(), last.getHubType()),
                segments,
                totalDuration,
                totalDistance
        );
    }
}
