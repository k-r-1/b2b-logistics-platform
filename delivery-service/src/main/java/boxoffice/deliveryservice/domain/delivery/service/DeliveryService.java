package boxoffice.deliveryservice.domain.delivery.service;

import boxoffice.deliveryservice.client.HubClient;
import boxoffice.deliveryservice.client.UserServiceClient;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto;
import boxoffice.deliveryservice.client.dto.response.UserResponseDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryStatusUpdateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryUpdateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.response.DeliveryResponseDto;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.delivery.exception.DeliveryErrorCode;
import boxoffice.deliveryservice.domain.delivery.repository.DeliveryRepository;
import boxoffice.deliveryservice.domain.deliveryroute.dto.request.DeliveryRouteStatusUpdateRequestDto;
import boxoffice.deliveryservice.domain.deliveryroute.dto.request.DeliveryRouteUpdateRequestDto;
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

import static boxoffice.deliveryservice.client.entity.UserRole.DELIVERY_MANAGER;
import static boxoffice.deliveryservice.client.entity.UserRole.HUB_MANAGER;
import static boxoffice.deliveryservice.client.entity.UserRole.MASTER;
import static boxoffice.deliveryservice.client.entity.UserRole.SUPPLIER_MANAGER;

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
        UserResponseDto userInfo = getUserInfo(keycloakSub);

        Page<Delivery> deliveries = switch (userInfo.getRole()) {
            case MASTER -> deliveryRepository.findAllByDeletedAtIsNull(pageable);
            case HUB_MANAGER -> deliveryRepository.findAllByHubIdAndDeletedAtIsNull(userInfo.getHubId(), pageable);
            case DELIVERY_MANAGER ->
                deliveryRepository.findAllByDeliveryPersonIdAndDeletedAtIsNull(userInfo.getId(), pageable);
            case SUPPLIER_MANAGER ->
                deliveryRepository.findAllByCompanyIdAndDeletedAtIsNull(userInfo.getCompanyId(), pageable);
        };

        return PageResponse.of(deliveries.map(DeliveryResponseDto::from));
    }

    @Transactional(readOnly = true)
    public DeliveryResponseDto getDelivery(String keycloakSub, UUID deliveryId) {
        UserResponseDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        checkDeliveryAccess(delivery, userInfo);
        return DeliveryResponseDto.from(delivery);
    }

    @Transactional(readOnly = true)
    public PageResponse<DeliveryRouteResponseDto> getDeliveryRoutes(
            String keycloakSub, UUID deliveryId, Pageable pageable) {
        UserResponseDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        checkDeliveryAccess(delivery, userInfo);
        return deliveryRouteService.getRoutesByDelivery(deliveryId, pageable);
    }

    @Transactional(readOnly = true)
    public DeliveryRouteResponseDto getDeliveryRoute(String keycloakSub, UUID deliveryId, UUID routeId) {
        UserResponseDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        checkDeliveryAccess(delivery, userInfo);
        return deliveryRouteService.getRouteByDelivery(deliveryId, routeId);
    }

    private UserResponseDto getUserInfo(String keycloakSub) {
        return userServiceClient.getUserBySub(keycloakSub).getData();
    }
    public DeliveryResponseDto updateDelivery(String keycloakSub, UUID deliveryId, DeliveryUpdateRequestDto request) {
        UserResponseDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = findDeliveryOrThrow(deliveryId);
        checkWriteAccess(delivery, userInfo);
        delivery.updateInfo(request.recipientName(), request.recipientSlackId(), request.deliveryAddress().toAddressVO());
        return DeliveryResponseDto.from(delivery);
    }

    public DeliveryResponseDto updateDeliveryStatus(String keycloakSub, UUID deliveryId, DeliveryStatusUpdateRequestDto request) {
        UserResponseDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = findDeliveryOrThrow(deliveryId);
        checkWriteAccess(delivery, userInfo);
        delivery.updateStatus(request.status());
        return DeliveryResponseDto.from(delivery);
    }

    public DeliveryRouteResponseDto updateDeliveryRoute(String keycloakSub, UUID deliveryId, UUID routeId, DeliveryRouteUpdateRequestDto request) {
        UserResponseDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = findDeliveryOrThrow(deliveryId);
        checkWriteAccess(delivery, userInfo);
        return deliveryRouteService.updateRoute(routeId, deliveryId, request);
    }

    public DeliveryRouteResponseDto updateDeliveryRouteStatus(String keycloakSub, UUID deliveryId, UUID routeId, DeliveryRouteStatusUpdateRequestDto request) {
        UserResponseDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = findDeliveryOrThrow(deliveryId);
        checkWriteAccess(delivery, userInfo);
        return deliveryRouteService.updateRouteStatus(routeId, deliveryId, request);
    }

    public void deleteDelivery(String keycloakSub, UUID deliveryId) {
        UserResponseDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = findDeliveryOrThrow(deliveryId);
        checkDeleteAccess(delivery, userInfo);
        deliveryRouteService.deleteAllByDelivery(deliveryId, userInfo.getId());
        delivery.softDelete(userInfo.getId());
    }

    public void deleteDeliveryRoute(String keycloakSub, UUID deliveryId, UUID routeId) {
        UserResponseDto userInfo = getUserInfo(keycloakSub);
        Delivery delivery = findDeliveryOrThrow(deliveryId);
        checkDeleteAccess(delivery, userInfo);
        deliveryRouteService.deleteRoute(routeId, deliveryId, userInfo.getId());
    }

    private Delivery findDeliveryOrThrow(UUID deliveryId) {
        return deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
    }

    private void checkWriteAccess(Delivery delivery, UserResponseDto userInfo) {
        switch (userInfo.getRole()) {
            case MASTER -> { }
            case HUB_MANAGER -> {
                if (userInfo.getHubId() == null ||
                    !userInfo.getHubId().equals(delivery.getOriginHubId()) &&
                    !userInfo.getHubId().equals(delivery.getDestinationHubId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            case DELIVERY_MANAGER -> {
                if (delivery.getDeliveryPersonId() == null ||
                    !userInfo.getId().equals(delivery.getDeliveryPersonId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            default -> throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void checkDeliveryAccess(Delivery delivery, UserResponseDto userInfo) {
        switch (userInfo.getRole()) {
            case MASTER -> { }
            case HUB_MANAGER -> {
                if (userInfo.getHubId() == null ||
                    !userInfo.getHubId().equals(delivery.getOriginHubId()) &&
                    !userInfo.getHubId().equals(delivery.getDestinationHubId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            case DELIVERY_MANAGER -> {
                if (delivery.getDeliveryPersonId() == null ||
                    !userInfo.getId().equals(delivery.getDeliveryPersonId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            case SUPPLIER_MANAGER -> {
                if (userInfo.getCompanyId() == null ||
                    !userInfo.getCompanyId().equals(delivery.getCompanyId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
        }
    }

    private void checkDeleteAccess(Delivery delivery, UserResponseDto userInfo) {
        switch (userInfo.getRole()) {
            case MASTER -> {}
            case HUB_MANAGER -> {
                if (userInfo.getHubId() == null ||
                        (!userInfo.getHubId().equals(delivery.getOriginHubId()) &&
                                !userInfo.getHubId().equals(delivery.getDestinationHubId()))) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            case DELIVERY_MANAGER, SUPPLIER_MANAGER -> throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }
}
