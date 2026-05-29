package boxoffice.deliveryservice.client.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HubRouteResponseDto(
        HubInfo originHub,
        HubInfo destinationHub,
        List<HubRouteSegmentDto> segments,
        int totalDurationMin,
        BigDecimal totalDistanceKm
) {
    public record HubInfo(UUID hubId, String name, HubType hubType) { }

    public record HubRouteSegmentDto(
            int sequence,
            HubInfo originHub,
            HubInfo destinationHub,
            int estimatedDurationMin,
            BigDecimal estimatedDistanceKm
    ) { }

    public enum HubType { CENTRAL, REGIONAL, CLOSING, INACTIVE }
}
