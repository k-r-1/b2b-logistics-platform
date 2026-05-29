package com.boxoffice.hubservice.hubroute.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.repository.HubRepository;
import com.boxoffice.hubservice.hubroute.dto.request.HubRouteCreateRequestDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteCreateResponseDto;
import com.boxoffice.hubservice.hubroute.entity.HubRoute;
import com.boxoffice.hubservice.hubroute.repository.HubRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteService {

    private final HubRouteRepository hubRouteRepository;
    private final HubRepository hubRepository;

    @Transactional
    public HubRouteCreateResponseDto createHubRoute(HubRouteCreateRequestDto request) {
        if (request.originHubId().equals(request.destinationHubId())) {
            throw new BaseException(HubErrorCode.SAME_HUB_ROUTE);
        }

        if (hubRouteRepository.existsByOriginHubIdAndDestinationHubId(
                request.originHubId(), request.destinationHubId())) {
            throw new BaseException(HubErrorCode.DUPLICATE_HUB_ROUTE);
        }

        Hub originHub = hubRepository.findById(request.originHubId())
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));
        Hub destinationHub = hubRepository.findById(request.destinationHubId())
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        if (originHub.isInactive() || originHub.isClosing()
                || destinationHub.isInactive() || destinationHub.isClosing()) {
            throw new BaseException(HubErrorCode.HUB_INACTIVE_IN_PATH);
        }

        HubRoute route = HubRoute.builder()
                .originHubId(request.originHubId())
                .destinationHubId(request.destinationHubId())
                .estimatedDurationMin(request.estimatedDurationMin())
                .estimatedDistanceKm(BigDecimal.valueOf(request.estimatedDistanceKm()))
                .build();

        return HubRouteCreateResponseDto.from(hubRouteRepository.save(route), originHub, destinationHub);
    }
}
