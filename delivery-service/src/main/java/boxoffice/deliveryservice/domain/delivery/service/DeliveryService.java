package boxoffice.deliveryservice.domain.delivery.service;

import boxoffice.deliveryservice.client.HubClient;
import boxoffice.deliveryservice.client.UserServiceClient;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto;
import boxoffice.deliveryservice.client.dto.response.UserInfoDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.response.DeliveryResponseDto;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.delivery.exception.DeliveryErrorCode;
import boxoffice.deliveryservice.domain.delivery.repository.DeliveryRepository;
import boxoffice.deliveryservice.domain.deliveryroute.dto.response.DeliveryRouteResponseDto;
import boxoffice.deliveryservice.domain.deliveryroute.service.DeliveryRouteService;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final UserServiceClient userServiceClient;

    public DeliveryResponseDto createDelivery(DeliveryCreateRequestDto request) {
        Delivery delivery = Delivery.create(
                request.orderId(),
                request.companyId(),
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
    public PageResponse<DeliveryResponseDto> getDeliveries(String keycloakSub, Pageable pageable) {
        UserInfoDto userInfo = getUserInfo(keycloakSub);

        Page<Delivery> deliveries = switch (userInfo.role()) {
            case MASTER -> deliveryRepository.findAllByDeletedAtIsNull(pageable);
            case HUB_MANAGER -> deliveryRepository.findAllByHubIdAndDeletedAtIsNull(userInfo.hubId(), pageable);
            case DELIVERY_MANAGER -> deliveryRepository.findAllByDeliveryPersonIdAndDeletedAtIsNull(userInfo.id(), pageable);
            case SUPPLIER_MANAGER -> deliveryRepository.findAllByCompanyIdAndDeletedAtIsNull(userInfo.companyId(), pageable);
        };

        return PageResponse.of(deliveries.map(DeliveryResponseDto::from));
    }

    @Transactional(readOnly = true)
    public DeliveryResponseDto getDelivery(String keycloakSub, UUID deliveryId) {
        UserInfoDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        checkDeliveryAccess(delivery, userInfo);
        return DeliveryResponseDto.from(delivery);
    }

    @Transactional(readOnly = true)
    public PageResponse<DeliveryRouteResponseDto> getDeliveryRoutes(String keycloakSub, UUID deliveryId, Pageable pageable) {
        UserInfoDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        checkDeliveryAccess(delivery, userInfo);
        return deliveryRouteService.getRoutesByDelivery(deliveryId, pageable);
    }

    @Transactional(readOnly = true)
    public DeliveryRouteResponseDto getDeliveryRoute(String keycloakSub, UUID deliveryId, UUID routeId) {
        UserInfoDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        checkDeliveryAccess(delivery, userInfo);
        return deliveryRouteService.getRouteByDelivery(deliveryId, routeId);
    }

    private UserInfoDto getUserInfo(String keycloakSub) {
        return userServiceClient.getUserBySub(keycloakSub).getData();
    }

    private void checkDeliveryAccess(Delivery delivery, UserInfoDto userInfo) {
        switch (userInfo.role()) {
            case MASTER -> {}
            case HUB_MANAGER -> {
                if (userInfo.hubId() == null ||
                    (!userInfo.hubId().equals(delivery.getOriginHubId()) &&
                     !userInfo.hubId().equals(delivery.getDestinationHubId()))) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            case DELIVERY_MANAGER -> {
                if (delivery.getDeliveryPersonId() == null ||
                    !userInfo.id().equals(delivery.getDeliveryPersonId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            case SUPPLIER_MANAGER -> {
                if (userInfo.companyId() == null ||
                    !userInfo.companyId().equals(delivery.getCompanyId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
        }
    }
}
