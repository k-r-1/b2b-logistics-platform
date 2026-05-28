package boxoffice.deliveryservice.domain.delivery.service;

import boxoffice.deliveryservice.client.HubClient;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.response.DeliveryResponseDto;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.delivery.exception.DeliveryErrorCode;
import boxoffice.deliveryservice.domain.delivery.repository.DeliveryRepository;
import boxoffice.deliveryservice.domain.deliveryroute.service.DeliveryRouteService;
import com.boxoffice.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
                request.deliveryAddress(),
                request.recipientName(),
                request.recipientSlackId()
        );
        deliveryRepository.save(delivery);

        HubRouteResponseDto hubRoute = hubClient.calculatePath(
                request.originHubId(),
                request.destinationHubId()
        ).getData();

        deliveryRouteService.createRoutes(delivery, hubRoute.segments());

        return DeliveryResponseDto.from(delivery);
    }

    public void cancelDelivery(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        if (delivery.isCanceled()) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_ALREADY_CANCELED);
        }
        delivery.cancel();
        delivery.softDelete(null);
    }
}
