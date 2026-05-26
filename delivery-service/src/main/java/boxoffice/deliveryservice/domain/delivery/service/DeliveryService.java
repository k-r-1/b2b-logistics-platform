package boxoffice.deliveryservice.domain.delivery.service;

import boxoffice.deliveryservice.client.HubClient;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.response.DeliveryResponseDto;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.delivery.exception.DeliveryErrorCode;
import boxoffice.deliveryservice.domain.delivery.repository.DeliveryRepository;
import boxoffice.deliveryservice.domain.deliveryroute.dto.response.DeliveryRouteResponseDto;
import boxoffice.deliveryservice.domain.deliveryroute.service.DeliveryRouteService;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public PageResponse<DeliveryResponseDto> getDeliveries(Pageable pageable) {
        return PageResponse.of(
                deliveryRepository.findAllByDeletedAtIsNull(pageable)
                        .map(DeliveryResponseDto::from)
        );
    }

    @Transactional(readOnly = true)
    public DeliveryResponseDto getDelivery(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        return DeliveryResponseDto.from(delivery);
    }

    @Transactional(readOnly = true)
    public PageResponse<DeliveryRouteResponseDto> getDeliveryRoutes(UUID deliveryId, Pageable pageable) {
        if (deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId).isEmpty()) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND);
        }
        return deliveryRouteService.getRoutesByDelivery(deliveryId, pageable);
    }

    @Transactional(readOnly = true)
    public DeliveryRouteResponseDto getDeliveryRoute(UUID deliveryId, UUID routeId) {
        if (deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId).isEmpty()) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND);
        }
        return deliveryRouteService.getRouteByDelivery(deliveryId, routeId);
    }

}
