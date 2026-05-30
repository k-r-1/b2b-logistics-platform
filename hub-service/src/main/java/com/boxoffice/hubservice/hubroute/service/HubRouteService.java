package com.boxoffice.hubservice.hubroute.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.common.util.PageableUtils;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hubroute.dto.request.HubRouteUpdateRequestDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRoutePathResponseDto;
import com.boxoffice.hubservice.hubroute.entity.QHubRoute;
import com.boxoffice.hubservice.hub.repository.HubRepository;
import com.boxoffice.hubservice.hubroute.dto.request.HubRouteCreateRequestDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteCreateResponseDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteGetResponseDto;
import com.boxoffice.hubservice.hubroute.entity.HubRoute;
import com.boxoffice.hubservice.hubroute.repository.HubRouteRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final AuditorAware<UUID> auditorAware;

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

    @Transactional
    @CacheEvict(cacheNames = "hub-routes", allEntries = true)
    public HubRouteGetResponseDto updateHubRoute(UUID routeId, HubRouteUpdateRequestDto request) {
        if (request.estimatedDurationMin() == null && request.estimatedDistanceKm() == null) {
            throw new BaseException(HubErrorCode.NO_FIELDS_TO_UPDATE);
        }

        HubRoute route = hubRouteRepository.findById(routeId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_ROUTE_NOT_FOUND));

        BigDecimal distanceKm = request.estimatedDistanceKm() != null
                ? BigDecimal.valueOf(request.estimatedDistanceKm())
                : null;

        route.update(request.estimatedDurationMin(), distanceKm);
        hubRouteRepository.saveAndFlush(route);

        Hub originHub = hubRepository.findById(route.getOriginHubId())
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));
        Hub destinationHub = hubRepository.findById(route.getDestinationHubId())
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        return HubRouteGetResponseDto.from(route, originHub, destinationHub);
    }

    @Transactional
    @CacheEvict(cacheNames = "hub-routes", allEntries = true)
    public void deleteHubRoute(UUID routeId) {
        HubRoute route = hubRouteRepository.findById(routeId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_ROUTE_NOT_FOUND));
        UUID deletedBy = auditorAware.getCurrentAuditor().orElse(null);
        route.softDelete(deletedBy);
    }

    @Cacheable(cacheNames = "hub-routes", key = "#originHubId + ':' + #destinationHubId")
    public HubRoutePathResponseDto calculatePath(UUID originHubId, UUID destinationHubId) {
        if (originHubId.equals(destinationHubId)) {
            throw new BaseException(HubErrorCode.SAME_HUB_PATH);
        }

        Hub origin = hubRepository.findById(originHubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));
        Hub destination = hubRepository.findById(destinationHubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        if (origin.isInactive() || origin.isClosing()
                || destination.isInactive() || destination.isClosing()) {
            throw new BaseException(HubErrorCode.HUB_INACTIVE_IN_PATH);
        }

        Optional<HubRoute> direct = hubRouteRepository
                .findByOriginHubIdAndDestinationHubId(originHubId, destinationHubId);
        if (direct.isPresent()) {
            return HubRoutePathResponseDto.of(
                    List.of(direct.get()),
                    Map.of(originHubId, origin, destinationHubId, destination));
        }

        Set<UUID> centralHubIds = hubRepository.findAllByHubType(HubType.CENTRAL).stream()
                .map(Hub::getId)
                .collect(Collectors.toSet());

        Hub originCentral = findCentralHub(origin, centralHubIds);
        Hub destCentral = findCentralHub(destination, centralHubIds);

        List<HubRoute> segments = new ArrayList<>();

        if (!origin.getId().equals(originCentral.getId())) {
            segments.add(hubRouteRepository
                    .findByOriginHubIdAndDestinationHubId(origin.getId(), originCentral.getId())
                    .orElseThrow(() -> new BaseException(HubErrorCode.HUB_ROUTE_NOT_FOUND)));
        }

        if (!originCentral.getId().equals(destCentral.getId())) {
            segments.add(hubRouteRepository
                    .findByOriginHubIdAndDestinationHubId(originCentral.getId(), destCentral.getId())
                    .orElseThrow(() -> new BaseException(HubErrorCode.HUB_ROUTE_NOT_FOUND)));
        }

        if (!destination.getId().equals(destCentral.getId())) {
            segments.add(hubRouteRepository
                    .findByOriginHubIdAndDestinationHubId(destCentral.getId(), destination.getId())
                    .orElseThrow(() -> new BaseException(HubErrorCode.HUB_ROUTE_NOT_FOUND)));
        }

        if (segments.isEmpty()) {
            throw new BaseException(HubErrorCode.HUB_ROUTE_NOT_FOUND);
        }

        Set<UUID> hubIds = segments.stream()
                .flatMap(r -> Stream.of(r.getOriginHubId(), r.getDestinationHubId()))
                .collect(Collectors.toSet());
        Map<UUID, Hub> hubMap = hubRepository.findAllById(hubIds).stream()
                .collect(Collectors.toMap(Hub::getId, Function.identity()));

        return HubRoutePathResponseDto.of(segments, hubMap);
    }

    private Hub findCentralHub(Hub hub, Set<UUID> centralHubIds) {
        if (hub.getHubType() == HubType.CENTRAL) {
            return hub;
        }

        UUID centralId = hubRouteRepository.findAllByOriginHubId(hub.getId()).stream()
                .map(HubRoute::getDestinationHubId)
                .filter(centralHubIds::contains)
                .findFirst()
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_ROUTE_NOT_FOUND));

        return hubRepository.findById(centralId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));
    }
}
