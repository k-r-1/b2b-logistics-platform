package com.boxoffice.hubservice.hubroute.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.common.util.PageableUtils;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hubroute.entity.QHubRoute;
import com.boxoffice.hubservice.hub.repository.HubRepository;
import com.boxoffice.hubservice.hubroute.dto.request.HubRouteCreateRequestDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteCreateResponseDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteGetResponseDto;
import com.boxoffice.hubservice.hubroute.entity.HubRoute;
import com.boxoffice.hubservice.hubroute.repository.HubRouteRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public HubRouteGetResponseDto getHubRoute(UUID routeId) {
        HubRoute route = hubRouteRepository.findById(routeId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_ROUTE_NOT_FOUND));

        Hub originHub = hubRepository.findById(route.getOriginHubId())
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));
        Hub destinationHub = hubRepository.findById(route.getDestinationHubId())
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        return HubRouteGetResponseDto.from(route, originHub, destinationHub);
    }

    public PageResponse<HubRouteGetResponseDto> getHubRoutes(
            UUID originHubId, UUID destinationHubId, int page, int size) {
        Pageable pageable = PageableUtils.ofDefault(page, size);

        QHubRoute qHubRoute = QHubRoute.hubRoute;
        BooleanBuilder builder = new BooleanBuilder();
        if (originHubId != null) {
            builder.and(qHubRoute.originHubId.eq(originHubId));
        }
        if (destinationHubId != null) {
            builder.and(qHubRoute.destinationHubId.eq(destinationHubId));
        }

        Page<HubRoute> routes = hubRouteRepository.findAll(builder, pageable);

        Set<UUID> hubIds = routes.stream()
                .flatMap(r -> Stream.of(r.getOriginHubId(), r.getDestinationHubId()))
                .collect(Collectors.toSet());

        Map<UUID, Hub> hubMap = hubRepository.findAllById(hubIds).stream()
                .collect(Collectors.toMap(Hub::getId, Function.identity()));

        return PageResponse.of(routes.map(r -> {
            Hub origin = hubMap.get(r.getOriginHubId());
            Hub destination = hubMap.get(r.getDestinationHubId());
            if (origin == null || destination == null) {
                throw new BaseException(HubErrorCode.HUB_NOT_FOUND);
            }
            return HubRouteGetResponseDto.from(r, origin, destination);
        }));
    }
}
