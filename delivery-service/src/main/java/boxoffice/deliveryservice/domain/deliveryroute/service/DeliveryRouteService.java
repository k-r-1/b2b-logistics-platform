package boxoffice.deliveryservice.domain.deliveryroute.service;

import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto.HubRouteSegmentDto;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRoute;
import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRouteStatus;
import boxoffice.deliveryservice.domain.deliveryroute.repository.DeliveryRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryRouteService {

    private final DeliveryRouteRepository deliveryRouteRepository;

    public void createRoutes(Delivery delivery, List<HubRouteSegmentDto> segments) {
        List<DeliveryRoute> routes = segments.stream()
                .map(segment -> DeliveryRoute.builder()
                        .delivery(delivery)
                        .originHubId(segment.originHub().hubId())
                        .destinationHubId(segment.destinationHub().hubId())
                        .sequence(segment.sequence())
                        .expectedDistance(segment.estimatedDistanceKm())
                        .expectedDuration(segment.estimatedDurationMin())
                        .status(DeliveryRouteStatus.WAITING)
                        .build())
                .toList();

        deliveryRouteRepository.saveAll(routes);
    }
}