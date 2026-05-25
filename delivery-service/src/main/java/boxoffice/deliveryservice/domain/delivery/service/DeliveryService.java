package boxoffice.deliveryservice.domain.delivery.service;

import boxoffice.deliveryservice.client.HubClient;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.response.DeliveryResponseDto;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.delivery.repository.DeliveryRepository;
import boxoffice.deliveryservice.domain.deliveryroute.service.DeliveryRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteService deliveryRouteService;
    private final HubClient hubClient;

    public DeliveryResponseDto createDelivery(DeliveryCreateRequestDto request) {
        Delivery delivery = Delivery.create(
                request.orderId(),
                request.originHubId(),
                request.destinationHubId(),
                request.deliveryAddress().toAddressVO(),
                request.recipientName(),
                request.recipientSlackId()
        );
        deliveryRepository.save(delivery);

        // TODO: delivery-manager-service 연동 후 hubDeliveryPersonId 배정
        HubRouteResponseDto hubRoute = hubClient.calculatePath(
                request.originHubId(),
                request.destinationHubId()
        );

        deliveryRouteService.createRoutes(delivery, hubRoute.segments());

        return DeliveryResponseDto.from(delivery);
    }
}