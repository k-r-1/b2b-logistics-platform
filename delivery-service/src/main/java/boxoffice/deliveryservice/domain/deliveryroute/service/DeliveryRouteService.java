package boxoffice.deliveryservice.domain.deliveryroute.service;

import boxoffice.deliveryservice.client.DeliveryManagerClient;
import boxoffice.deliveryservice.client.dto.request.DeliveryManagerAssignRequestDto;
import boxoffice.deliveryservice.client.dto.request.DeliveryManagerAssignRequestDto.DeliveryType;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto.HubRouteSegmentDto;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.deliveryroute.dto.response.DeliveryRouteResponseDto;
import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRoute;
import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRouteStatus;
import boxoffice.deliveryservice.domain.deliveryroute.exception.DeliveryRouteErrorCode;
import boxoffice.deliveryservice.domain.deliveryroute.repository.DeliveryRouteRepository;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryRouteService {

    private final DeliveryRouteRepository deliveryRouteRepository;
    private final DeliveryManagerClient deliveryManagerClient;

    public void createRoutes(Delivery delivery, List<HubRouteSegmentDto> segments) {
        List<DeliveryRoute> routes = segments.stream()
                .map(segment -> {
                    UUID managerId = deliveryManagerClient.assignDeliveryManager(
                            new DeliveryManagerAssignRequestDto(segment.originHub().hubId(), DeliveryType.HUB_TO_HUB)
                    ).getData().deliveryManagerId();

                    return DeliveryRoute.builder()
                            .delivery(delivery)
                            .originHubId(segment.originHub().hubId())
                            .destinationHubId(segment.destinationHub().hubId())
                            .sequence(segment.sequence())
                            .expectedDistance(segment.estimatedDistanceKm())
                            .expectedDuration(segment.estimatedDurationMin())
                            .hubDeliveryPersonId(managerId)
                            .status(DeliveryRouteStatus.WAITING)
                            .build();
                })
                .toList();

        deliveryRouteRepository.saveAll(routes);
    }

    @Transactional(readOnly = true)
    public PageResponse<DeliveryRouteResponseDto> getRoutesByDelivery(UUID deliveryId, Pageable pageable) {
        return PageResponse.of(
                deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNull(deliveryId, pageable)
                        .map(DeliveryRouteResponseDto::from)
        );
    }

    @Transactional(readOnly = true)
    public DeliveryRouteResponseDto getRouteByDelivery(UUID deliveryId, UUID routeId) {
        DeliveryRoute route = deliveryRouteRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(routeId, deliveryId)
                .orElseThrow(() -> new BaseException(DeliveryRouteErrorCode.DELIVERY_ROUTE_NOT_FOUND));
        return DeliveryRouteResponseDto.from(route);
    }
}
