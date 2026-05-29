package com.boxoffice.hubservice.hubroute.service;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.hub.entity.CoordinateVO;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hub.repository.HubRepository;
import com.boxoffice.hubservice.hubroute.dto.request.HubRouteCreateRequestDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteCreateResponseDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteGetResponseDto;
import com.boxoffice.hubservice.hubroute.entity.HubRoute;
import com.boxoffice.hubservice.hubroute.repository.HubRouteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HubRouteServiceTest {

    @InjectMocks
    private HubRouteService hubRouteService;

    @Mock
    private HubRouteRepository hubRouteRepository;

    @Mock
    private HubRepository hubRepository;

    private Hub buildHub(String name, HubType hubType) {
        Hub hub = Hub.builder()
                .name(name)
                .address(new AddressVO("12345", "서울시 강남구", null))
                .coordinate(new CoordinateVO(37.5, 127.0))
                .hubType(hubType)
                .build();
        ReflectionTestUtils.setField(hub, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(hub, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(hub, "updatedAt", LocalDateTime.now());
        return hub;
    }

    private HubRoute buildSavedRoute(UUID originHubId, UUID destinationHubId) {
        HubRoute route = HubRoute.builder()
                .originHubId(originHubId)
                .destinationHubId(destinationHubId)
                .estimatedDurationMin(120)
                .estimatedDistanceKm(BigDecimal.valueOf(160.5))
                .build();
        ReflectionTestUtils.setField(route, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(route, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(route, "updatedAt", LocalDateTime.now());
        return route;
    }

    @Test
    @DisplayName("허브 경로 생성 성공")
    void createHubRoute_success() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        UUID originId = origin.getId();
        UUID destinationId = destination.getId();
        HubRouteCreateRequestDto request = new HubRouteCreateRequestDto(originId, destinationId, 120, 160.5);
        HubRoute savedRoute = buildSavedRoute(originId, destinationId);

        given(hubRouteRepository.existsByOriginHubIdAndDestinationHubId(originId, destinationId)).willReturn(false);
        given(hubRepository.findById(originId)).willReturn(Optional.of(origin));
        given(hubRepository.findById(destinationId)).willReturn(Optional.of(destination));
        given(hubRouteRepository.save(any(HubRoute.class))).willReturn(savedRoute);

        // when
        HubRouteCreateResponseDto response = hubRouteService.createHubRoute(request);

        // then
        assertThat(response.originHub().hubId()).isEqualTo(originId);
        assertThat(response.originHub().name()).isEqualTo("서울특별시 센터");
        assertThat(response.destinationHub().hubId()).isEqualTo(destinationId);
        assertThat(response.estimatedDurationMin()).isEqualTo(120);
        assertThat(response.estimatedDistanceKm()).isEqualTo(160.5);
        verify(hubRouteRepository).save(any(HubRoute.class));
    }

    @Test
    @DisplayName("출발 허브와 도착 허브가 동일하면 예외 발생")
    void createHubRoute_sameHub_throwsException() {
        // given
        UUID sameId = UUID.randomUUID();
        HubRouteCreateRequestDto request = new HubRouteCreateRequestDto(sameId, sameId, 120, 160.5);

        // when & then
        assertThatThrownBy(() -> hubRouteService.createHubRoute(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.SAME_HUB_ROUTE));
        verify(hubRouteRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 존재하는 경로면 예외 발생")
    void createHubRoute_duplicateRoute_throwsException() {
        // given
        UUID originId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();
        HubRouteCreateRequestDto request = new HubRouteCreateRequestDto(originId, destinationId, 120, 160.5);

        given(hubRouteRepository.existsByOriginHubIdAndDestinationHubId(originId, destinationId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> hubRouteService.createHubRoute(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.DUPLICATE_HUB_ROUTE));
        verify(hubRouteRepository, never()).save(any());
    }

    @Test
    @DisplayName("출발 허브가 존재하지 않으면 예외 발생")
    void createHubRoute_originNotFound_throwsException() {
        // given
        UUID originId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();
        HubRouteCreateRequestDto request = new HubRouteCreateRequestDto(originId, destinationId, 120, 160.5);

        given(hubRouteRepository.existsByOriginHubIdAndDestinationHubId(originId, destinationId)).willReturn(false);
        given(hubRepository.findById(originId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubRouteService.createHubRoute(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("도착 허브가 존재하지 않으면 예외 발생")
    void createHubRoute_destinationNotFound_throwsException() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        UUID originId = origin.getId();
        UUID destinationId = UUID.randomUUID();
        HubRouteCreateRequestDto request = new HubRouteCreateRequestDto(originId, destinationId, 120, 160.5);

        given(hubRouteRepository.existsByOriginHubIdAndDestinationHubId(originId, destinationId)).willReturn(false);
        given(hubRepository.findById(originId)).willReturn(Optional.of(origin));
        given(hubRepository.findById(destinationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubRouteService.createHubRoute(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("출발 허브가 CLOSING 상태면 예외 발생")
    void createHubRoute_originClosing_throwsException() {
        // given
        Hub closingOrigin = buildHub("마감 예정 센터", HubType.REGIONAL);
        closingOrigin.startClosing("이전 예정");
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        UUID originId = closingOrigin.getId();
        UUID destinationId = destination.getId();
        HubRouteCreateRequestDto request = new HubRouteCreateRequestDto(originId, destinationId, 120, 160.5);

        given(hubRouteRepository.existsByOriginHubIdAndDestinationHubId(originId, destinationId)).willReturn(false);
        given(hubRepository.findById(originId)).willReturn(Optional.of(closingOrigin));
        given(hubRepository.findById(destinationId)).willReturn(Optional.of(destination));

        // when & then
        assertThatThrownBy(() -> hubRouteService.createHubRoute(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_INACTIVE_IN_PATH));
        verify(hubRouteRepository, never()).save(any());
    }

    @Test
    @DisplayName("출발 허브가 INACTIVE 상태면 예외 발생")
    void createHubRoute_originInactive_throwsException() {
        // given
        Hub inactiveOrigin = buildHub("폐쇄된 센터", HubType.REGIONAL);
        inactiveOrigin.startClosing("운영 종료");
        inactiveOrigin.deactivate();
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        UUID originId = inactiveOrigin.getId();
        UUID destinationId = destination.getId();
        HubRouteCreateRequestDto request = new HubRouteCreateRequestDto(originId, destinationId, 120, 160.5);

        given(hubRouteRepository.existsByOriginHubIdAndDestinationHubId(originId, destinationId)).willReturn(false);
        given(hubRepository.findById(originId)).willReturn(Optional.of(inactiveOrigin));
        given(hubRepository.findById(destinationId)).willReturn(Optional.of(destination));

        // when & then
        assertThatThrownBy(() -> hubRouteService.createHubRoute(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_INACTIVE_IN_PATH));
        verify(hubRouteRepository, never()).save(any());
    }

    @Test
    @DisplayName("도착 허브가 INACTIVE 상태면 예외 발생")
    void createHubRoute_destinationInactive_throwsException() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        Hub inactiveDestination = buildHub("폐쇄된 센터", HubType.REGIONAL);
        inactiveDestination.startClosing("운영 종료");
        inactiveDestination.deactivate();
        UUID originId = origin.getId();
        UUID destinationId = inactiveDestination.getId();
        HubRouteCreateRequestDto request = new HubRouteCreateRequestDto(originId, destinationId, 120, 160.5);

        given(hubRouteRepository.existsByOriginHubIdAndDestinationHubId(originId, destinationId)).willReturn(false);
        given(hubRepository.findById(originId)).willReturn(Optional.of(origin));
        given(hubRepository.findById(destinationId)).willReturn(Optional.of(inactiveDestination));

        // when & then
        assertThatThrownBy(() -> hubRouteService.createHubRoute(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_INACTIVE_IN_PATH));
        verify(hubRouteRepository, never()).save(any());
    }

    @Test
    @DisplayName("도착 허브가 CLOSING 상태면 예외 발생")
    void createHubRoute_destinationClosing_throwsException() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        Hub closingDestination = buildHub("마감 예정 센터", HubType.REGIONAL);
        closingDestination.startClosing("이전 예정");
        UUID originId = origin.getId();
        UUID destinationId = closingDestination.getId();
        HubRouteCreateRequestDto request = new HubRouteCreateRequestDto(originId, destinationId, 120, 160.5);

        given(hubRouteRepository.existsByOriginHubIdAndDestinationHubId(originId, destinationId)).willReturn(false);
        given(hubRepository.findById(originId)).willReturn(Optional.of(origin));
        given(hubRepository.findById(destinationId)).willReturn(Optional.of(closingDestination));

        // when & then
        assertThatThrownBy(() -> hubRouteService.createHubRoute(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_INACTIVE_IN_PATH));
        verify(hubRouteRepository, never()).save(any());
    }

    @Test
    @DisplayName("허브 경로 단건 조회 성공")
    void getHubRoute_success() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        HubRoute route = buildSavedRoute(origin.getId(), destination.getId());
        UUID routeId = route.getId();

        given(hubRouteRepository.findById(routeId)).willReturn(Optional.of(route));
        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));

        // when
        HubRouteGetResponseDto response = hubRouteService.getHubRoute(routeId);

        // then
        assertThat(response.routeId()).isEqualTo(routeId);
        assertThat(response.originHub().hubId()).isEqualTo(origin.getId());
        assertThat(response.destinationHub().hubId()).isEqualTo(destination.getId());
        assertThat(response.estimatedDurationMin()).isEqualTo(120);
    }

    @Test
    @DisplayName("존재하지 않는 경로 단건 조회 시 예외 발생")
    void getHubRoute_routeNotFound_throwsException() {
        // given
        UUID routeId = UUID.randomUUID();
        given(hubRouteRepository.findById(routeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubRouteService.getHubRoute(routeId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_ROUTE_NOT_FOUND));
        verify(hubRepository, never()).findById(any());
    }

    @Test
    @DisplayName("출발 허브가 삭제된 경우 단건 조회 시 예외 발생")
    void getHubRoute_originHubNotFound_throwsException() {
        // given
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        UUID originId = UUID.randomUUID();
        HubRoute route = buildSavedRoute(originId, destination.getId());

        given(hubRouteRepository.findById(route.getId())).willReturn(Optional.of(route));
        given(hubRepository.findById(originId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubRouteService.getHubRoute(route.getId()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("도착 허브가 삭제된 경우 단건 조회 시 예외 발생")
    void getHubRoute_destinationHubNotFound_throwsException() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        UUID destinationId = UUID.randomUUID();
        HubRoute route = buildSavedRoute(origin.getId(), destinationId);

        given(hubRouteRepository.findById(route.getId())).willReturn(Optional.of(route));
        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destinationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubRouteService.getHubRoute(route.getId()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("허브 경로 목록 조회 - 필터 없음")
    void getHubRoutes_noFilter_returnsAll() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        HubRoute route = buildSavedRoute(origin.getId(), destination.getId());

        given(hubRouteRepository.findAll(any(Predicate.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(route)));
        given(hubRepository.findAllById(any()))
                .willReturn(List.of(origin, destination));

        // when
        PageResponse<HubRouteGetResponseDto> response = hubRouteService.getHubRoutes(null, null, 0, 10);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).originHub().hubId()).isEqualTo(origin.getId());
        assertThat(response.getContent().get(0).destinationHub().hubId()).isEqualTo(destination.getId());
    }

    @Test
    @DisplayName("허브 경로 목록 조회 - 출발 허브 필터 적용")
    void getHubRoutes_filteredByOriginHubId() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        HubRoute route = buildSavedRoute(origin.getId(), destination.getId());

        given(hubRouteRepository.findAll(any(Predicate.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(route)));
        given(hubRepository.findAllById(any()))
                .willReturn(List.of(origin, destination));

        // when
        PageResponse<HubRouteGetResponseDto> response = hubRouteService.getHubRoutes(origin.getId(), null, 0, 10);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).originHub().hubId()).isEqualTo(origin.getId());
    }

    @Test
    @DisplayName("허브 경로 목록 조회 - 도착 허브 필터 적용")
    void getHubRoutes_filteredByDestinationHubId() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        HubRoute route = buildSavedRoute(origin.getId(), destination.getId());

        given(hubRouteRepository.findAll(any(Predicate.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(route)));
        given(hubRepository.findAllById(any()))
                .willReturn(List.of(origin, destination));

        // when
        PageResponse<HubRouteGetResponseDto> response = hubRouteService.getHubRoutes(null, destination.getId(), 0, 10);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).destinationHub().hubId()).isEqualTo(destination.getId());
    }

    @Test
    @DisplayName("허브 경로 목록 조회 - 결과 없음")
    void getHubRoutes_emptyResult() {
        // given
        given(hubRouteRepository.findAll(any(Predicate.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(Collections.emptyList()));
        given(hubRepository.findAllById(any()))
                .willReturn(Collections.emptyList());

        // when
        PageResponse<HubRouteGetResponseDto> response = hubRouteService.getHubRoutes(null, null, 0, 10);

        // then
        assertThat(response.getContent()).isEmpty();
    }
}
