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
import com.boxoffice.hubservice.hubroute.dto.request.HubRouteUpdateRequestDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteCreateResponseDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRouteGetResponseDto;
import com.boxoffice.hubservice.hubroute.dto.response.HubRoutePathResponseDto;
import com.boxoffice.hubservice.hubroute.entity.HubRoute;
import com.boxoffice.hubservice.hubroute.repository.HubRouteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.AuditorAware;
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

    @Mock
    private AuditorAware<UUID> auditorAware;

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
        Hub destination = buildHub("인천광역시 센터", HubType.REGIONAL);
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

    @Test
    @DisplayName("허브 경로 수정 성공 - 두 필드 모두 수정")
    void updateHubRoute_success_bothFields() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        HubRoute route = buildSavedRoute(origin.getId(), destination.getId());
        HubRouteUpdateRequestDto request = new HubRouteUpdateRequestDto(90, 130.0);

        given(hubRouteRepository.findById(route.getId())).willReturn(Optional.of(route));
        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));

        // when
        HubRouteGetResponseDto response = hubRouteService.updateHubRoute(route.getId(), request);

        // then
        assertThat(response.estimatedDurationMin()).isEqualTo(90);
        assertThat(response.estimatedDistanceKm()).isEqualTo(130.0);
    }

    @Test
    @DisplayName("허브 경로 수정 성공 - 소요 시간만 수정")
    void updateHubRoute_success_durationOnly() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        HubRoute route = buildSavedRoute(origin.getId(), destination.getId());
        HubRouteUpdateRequestDto request = new HubRouteUpdateRequestDto(90, null);

        given(hubRouteRepository.findById(route.getId())).willReturn(Optional.of(route));
        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));

        // when
        HubRouteGetResponseDto response = hubRouteService.updateHubRoute(route.getId(), request);

        // then
        assertThat(response.estimatedDurationMin()).isEqualTo(90);
        assertThat(response.estimatedDistanceKm()).isEqualTo(160.5); // 기존 값 유지
    }

    @Test
    @DisplayName("허브 경로 수정 성공 - 거리만 수정")
    void updateHubRoute_success_distanceOnly() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        HubRoute route = buildSavedRoute(origin.getId(), destination.getId());
        HubRouteUpdateRequestDto request = new HubRouteUpdateRequestDto(null, 130.0);

        given(hubRouteRepository.findById(route.getId())).willReturn(Optional.of(route));
        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));

        // when
        HubRouteGetResponseDto response = hubRouteService.updateHubRoute(route.getId(), request);

        // then
        assertThat(response.estimatedDurationMin()).isEqualTo(120); // 기존 값 유지
        assertThat(response.estimatedDistanceKm()).isEqualTo(130.0);
    }

    @Test
    @DisplayName("수정할 필드가 없으면 예외 발생")
    void updateHubRoute_noFieldsToUpdate_throwsException() {
        // given
        UUID routeId = UUID.randomUUID();
        HubRouteUpdateRequestDto request = new HubRouteUpdateRequestDto(null, null);

        // when & then
        assertThatThrownBy(() -> hubRouteService.updateHubRoute(routeId, request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.NO_FIELDS_TO_UPDATE));
        verify(hubRouteRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 경로 수정 시 예외 발생")
    void updateHubRoute_routeNotFound_throwsException() {
        // given
        UUID routeId = UUID.randomUUID();
        HubRouteUpdateRequestDto request = new HubRouteUpdateRequestDto(90, 130.0);
        given(hubRouteRepository.findById(routeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubRouteService.updateHubRoute(routeId, request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_ROUTE_NOT_FOUND));
        verify(hubRepository, never()).findById(any());
    }

    @Test
    @DisplayName("출발 허브가 삭제된 경우 수정 시 예외 발생")
    void updateHubRoute_originHubNotFound_throwsException() {
        // given
        UUID originId = UUID.randomUUID();
        Hub destination = buildHub("대전광역시 센터", HubType.REGIONAL);
        HubRoute route = buildSavedRoute(originId, destination.getId());
        HubRouteUpdateRequestDto request = new HubRouteUpdateRequestDto(90, 130.0);

        given(hubRouteRepository.findById(route.getId())).willReturn(Optional.of(route));
        given(hubRepository.findById(originId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubRouteService.updateHubRoute(route.getId(), request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("도착 허브가 삭제된 경우 수정 시 예외 발생")
    void updateHubRoute_destinationHubNotFound_throwsException() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.CENTRAL);
        UUID destinationId = UUID.randomUUID();
        HubRoute route = buildSavedRoute(origin.getId(), destinationId);
        HubRouteUpdateRequestDto request = new HubRouteUpdateRequestDto(90, 130.0);

        given(hubRouteRepository.findById(route.getId())).willReturn(Optional.of(route));
        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destinationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubRouteService.updateHubRoute(route.getId(), request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("허브 경로 삭제 성공")
    void deleteHubRoute_success() {
        // given
        HubRoute route = buildSavedRoute(UUID.randomUUID(), UUID.randomUUID());
        given(hubRouteRepository.findById(route.getId())).willReturn(Optional.of(route));
        given(auditorAware.getCurrentAuditor()).willReturn(Optional.empty());

        hubRouteService.deleteHubRoute(route.getId());

        assertThat(route.isDeleted()).isTrue();
        assertThat(route.getDeletedAt()).isNotNull();
        assertThat(route.getDeletedBy()).isNull();
    }

    @Test
    @DisplayName("허브 경로 삭제 성공 - 감사자 정보가 있으면 deletedBy가 기록된다")
    void deleteHubRoute_success_withAuditor() {
        // given
        UUID auditorId = UUID.randomUUID();
        HubRoute route = buildSavedRoute(UUID.randomUUID(), UUID.randomUUID());
        given(hubRouteRepository.findById(route.getId())).willReturn(Optional.of(route));
        given(auditorAware.getCurrentAuditor()).willReturn(Optional.of(auditorId));

        // when
        hubRouteService.deleteHubRoute(route.getId());

        // then
        assertThat(route.isDeleted()).isTrue();
        assertThat(route.getDeletedAt()).isNotNull();
        assertThat(route.getDeletedBy()).isEqualTo(auditorId);
    }

    @Test
    @DisplayName("존재하지 않는 경로 삭제 시 예외 발생")
    void deleteHubRoute_routeNotFound_throwsException() {
        // given
        UUID routeId = UUID.randomUUID();
        given(hubRouteRepository.findById(routeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubRouteService.deleteHubRoute(routeId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_ROUTE_NOT_FOUND));
    }

    @Test
    @DisplayName("직접 경로 존재 시 1구간 반환 성공")
    void calculatePath_directRoute_success() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.REGIONAL);
        Hub destination = buildHub("인천광역시 센터", HubType.REGIONAL);
        HubRoute directRoute = buildSavedRoute(origin.getId(), destination.getId());

        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(origin.getId(), destination.getId()))
                .willReturn(Optional.of(directRoute));

        // when
        HubRoutePathResponseDto result = hubRouteService.calculatePath(origin.getId(), destination.getId());

        // then
        assertThat(result.segments()).hasSize(1);
        assertThat(result.segments().get(0).sequence()).isEqualTo(1);
        assertThat(result.originHub().hubId()).isEqualTo(origin.getId());
        assertThat(result.destinationHub().hubId()).isEqualTo(destination.getId());
        assertThat(result.totalDurationMin()).isEqualTo(120);
        verify(hubRepository, never()).findAllByHubType(any());
    }

    @Test
    @DisplayName("같은 그룹 REGIONAL → REGIONAL Hub&Spoke 경로 계산 성공 (2구간)")
    void calculatePath_sameGroupRegionalToRegional_success() {
        // given
        Hub central = buildHub("경기남부 센터", HubType.CENTRAL);
        Hub origin = buildHub("서울특별시 센터", HubType.REGIONAL);
        Hub destination = buildHub("인천광역시 센터", HubType.REGIONAL);
        HubRoute routeOriginToCentral = buildSavedRoute(origin.getId(), central.getId());
        HubRoute routeDestToCentral = buildSavedRoute(destination.getId(), central.getId());
        HubRoute routeCentralToDest = buildSavedRoute(central.getId(), destination.getId());

        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(origin.getId(), destination.getId()))
                .willReturn(Optional.empty());
        given(hubRepository.findAllByHubType(HubType.CENTRAL)).willReturn(List.of(central));
        given(hubRouteRepository.findAllByOriginHubId(origin.getId()))
                .willReturn(List.of(routeOriginToCentral));
        given(hubRepository.findById(central.getId())).willReturn(Optional.of(central));
        given(hubRouteRepository.findAllByOriginHubId(destination.getId()))
                .willReturn(List.of(routeDestToCentral));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(origin.getId(), central.getId()))
                .willReturn(Optional.of(routeOriginToCentral));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(central.getId(), destination.getId()))
                .willReturn(Optional.of(routeCentralToDest));
        given(hubRepository.findAllById(any())).willReturn(List.of(origin, central, destination));

        // when
        HubRoutePathResponseDto result = hubRouteService.calculatePath(origin.getId(), destination.getId());

        // then
        assertThat(result.segments()).hasSize(2);
        assertThat(result.segments().get(0).sequence()).isEqualTo(1);
        assertThat(result.segments().get(0).originHub().hubId()).isEqualTo(origin.getId());
        assertThat(result.segments().get(0).destinationHub().hubId()).isEqualTo(central.getId());
        assertThat(result.segments().get(1).sequence()).isEqualTo(2);
        assertThat(result.segments().get(1).originHub().hubId()).isEqualTo(central.getId());
        assertThat(result.segments().get(1).destinationHub().hubId()).isEqualTo(destination.getId());
        assertThat(result.totalDurationMin()).isEqualTo(240);
    }

    @Test
    @DisplayName("다른 그룹 REGIONAL → REGIONAL Hub&Spoke 경로 계산 성공 (3구간)")
    void calculatePath_differentGroupRegionalToRegional_success() {
        // given
        Hub central1 = buildHub("경기남부 센터", HubType.CENTRAL);
        Hub central2 = buildHub("대구광역시 센터", HubType.CENTRAL);
        Hub origin = buildHub("서울특별시 센터", HubType.REGIONAL);
        Hub destination = buildHub("부산광역시 센터", HubType.REGIONAL);
        HubRoute routeOriginToCentral1 = buildSavedRoute(origin.getId(), central1.getId());
        HubRoute routeDestToCentral2 = buildSavedRoute(destination.getId(), central2.getId());
        HubRoute routeCentral1ToCentral2 = buildSavedRoute(central1.getId(), central2.getId());
        HubRoute routeCentral2ToDest = buildSavedRoute(central2.getId(), destination.getId());

        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(origin.getId(), destination.getId()))
                .willReturn(Optional.empty());
        given(hubRepository.findAllByHubType(HubType.CENTRAL)).willReturn(List.of(central1, central2));
        given(hubRouteRepository.findAllByOriginHubId(origin.getId()))
                .willReturn(List.of(routeOriginToCentral1));
        given(hubRepository.findById(central1.getId())).willReturn(Optional.of(central1));
        given(hubRouteRepository.findAllByOriginHubId(destination.getId()))
                .willReturn(List.of(routeDestToCentral2));
        given(hubRepository.findById(central2.getId())).willReturn(Optional.of(central2));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(origin.getId(), central1.getId()))
                .willReturn(Optional.of(routeOriginToCentral1));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(central1.getId(), central2.getId()))
                .willReturn(Optional.of(routeCentral1ToCentral2));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(central2.getId(), destination.getId()))
                .willReturn(Optional.of(routeCentral2ToDest));
        given(hubRepository.findAllById(any())).willReturn(List.of(origin, central1, central2, destination));

        // when
        HubRoutePathResponseDto result = hubRouteService.calculatePath(origin.getId(), destination.getId());

        // then
        assertThat(result.segments()).hasSize(3);
        assertThat(result.segments().get(0).originHub().hubId()).isEqualTo(origin.getId());
        assertThat(result.segments().get(0).destinationHub().hubId()).isEqualTo(central1.getId());
        assertThat(result.segments().get(1).originHub().hubId()).isEqualTo(central1.getId());
        assertThat(result.segments().get(1).destinationHub().hubId()).isEqualTo(central2.getId());
        assertThat(result.segments().get(2).originHub().hubId()).isEqualTo(central2.getId());
        assertThat(result.segments().get(2).destinationHub().hubId()).isEqualTo(destination.getId());
        assertThat(result.totalDurationMin()).isEqualTo(360);
    }

    @Test
    @DisplayName("REGIONAL → 자신의 CENTRAL 경로 계산 성공 (1구간)")
    void calculatePath_regionalToOwnCentral_success() {
        // given
        Hub central = buildHub("경기남부 센터", HubType.CENTRAL);
        Hub origin = buildHub("서울특별시 센터", HubType.REGIONAL);
        HubRoute routeOriginToCentral = buildSavedRoute(origin.getId(), central.getId());

        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(central.getId())).willReturn(Optional.of(central));
        // 직접 경로 없음 → 두 번째 호출(구간 조립)에서 반환
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(origin.getId(), central.getId()))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(routeOriginToCentral));
        given(hubRepository.findAllByHubType(HubType.CENTRAL)).willReturn(List.of(central));
        given(hubRouteRepository.findAllByOriginHubId(origin.getId()))
                .willReturn(List.of(routeOriginToCentral));
        given(hubRepository.findAllById(any())).willReturn(List.of(origin, central));

        // when
        HubRoutePathResponseDto result = hubRouteService.calculatePath(origin.getId(), central.getId());

        // then
        assertThat(result.segments()).hasSize(1);
        assertThat(result.segments().get(0).originHub().hubId()).isEqualTo(origin.getId());
        assertThat(result.segments().get(0).destinationHub().hubId()).isEqualTo(central.getId());
    }

    @Test
    @DisplayName("CENTRAL → 같은 그룹 REGIONAL 경로 계산 성공 (1구간)")
    void calculatePath_centralToOwnGroupRegional_success() {
        // given
        Hub central = buildHub("경기남부 센터", HubType.CENTRAL);
        Hub destination = buildHub("서울특별시 센터", HubType.REGIONAL);
        HubRoute routeCentralToDest = buildSavedRoute(central.getId(), destination.getId());
        HubRoute routeDestToCentral = buildSavedRoute(destination.getId(), central.getId());

        given(hubRepository.findById(central.getId())).willReturn(Optional.of(central));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));
        // 직접 경로 없음 → 두 번째 호출(구간 조립)에서 반환
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(central.getId(), destination.getId()))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(routeCentralToDest));
        given(hubRepository.findAllByHubType(HubType.CENTRAL)).willReturn(List.of(central));
        given(hubRouteRepository.findAllByOriginHubId(destination.getId()))
                .willReturn(List.of(routeDestToCentral));
        given(hubRepository.findAllById(any())).willReturn(List.of(central, destination));

        // when
        HubRoutePathResponseDto result = hubRouteService.calculatePath(central.getId(), destination.getId());

        // then
        assertThat(result.segments()).hasSize(1);
        assertThat(result.segments().get(0).originHub().hubId()).isEqualTo(central.getId());
        assertThat(result.segments().get(0).destinationHub().hubId()).isEqualTo(destination.getId());
    }

    @Test
    @DisplayName("CENTRAL → 다른 그룹 REGIONAL 경로 계산 성공 (2구간)")
    void calculatePath_centralToDifferentGroupRegional_success() {
        // given
        Hub central1 = buildHub("경기남부 센터", HubType.CENTRAL);
        Hub central2 = buildHub("대구광역시 센터", HubType.CENTRAL);
        Hub destination = buildHub("부산광역시 센터", HubType.REGIONAL);
        HubRoute routeDestToCentral2 = buildSavedRoute(destination.getId(), central2.getId());
        HubRoute routeCentral1ToCentral2 = buildSavedRoute(central1.getId(), central2.getId());
        HubRoute routeCentral2ToDest = buildSavedRoute(central2.getId(), destination.getId());

        given(hubRepository.findById(central1.getId())).willReturn(Optional.of(central1));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(central1.getId(), destination.getId()))
                .willReturn(Optional.empty());
        given(hubRepository.findAllByHubType(HubType.CENTRAL)).willReturn(List.of(central1, central2));
        given(hubRouteRepository.findAllByOriginHubId(destination.getId()))
                .willReturn(List.of(routeDestToCentral2));
        given(hubRepository.findById(central2.getId())).willReturn(Optional.of(central2));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(central1.getId(), central2.getId()))
                .willReturn(Optional.of(routeCentral1ToCentral2));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(central2.getId(), destination.getId()))
                .willReturn(Optional.of(routeCentral2ToDest));
        given(hubRepository.findAllById(any())).willReturn(List.of(central1, central2, destination));

        // when
        HubRoutePathResponseDto result = hubRouteService.calculatePath(central1.getId(), destination.getId());

        // then
        assertThat(result.segments()).hasSize(2);
        assertThat(result.segments().get(0).originHub().hubId()).isEqualTo(central1.getId());
        assertThat(result.segments().get(0).destinationHub().hubId()).isEqualTo(central2.getId());
        assertThat(result.segments().get(1).originHub().hubId()).isEqualTo(central2.getId());
        assertThat(result.segments().get(1).destinationHub().hubId()).isEqualTo(destination.getId());
        assertThat(result.totalDurationMin()).isEqualTo(240);
    }

    @Test
    @DisplayName("CENTRAL → CENTRAL Hub&Spoke 경로 계산 성공 (1구간)")
    void calculatePath_centralToCentral_success() {
        // given
        Hub central1 = buildHub("경기남부 센터", HubType.CENTRAL);
        Hub central2 = buildHub("대구광역시 센터", HubType.CENTRAL);
        HubRoute routeCentral1ToCentral2 = buildSavedRoute(central1.getId(), central2.getId());

        given(hubRepository.findById(central1.getId())).willReturn(Optional.of(central1));
        given(hubRepository.findById(central2.getId())).willReturn(Optional.of(central2));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(central1.getId(), central2.getId()))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(routeCentral1ToCentral2));
        given(hubRepository.findAllByHubType(HubType.CENTRAL)).willReturn(List.of(central1, central2));
        given(hubRepository.findAllById(any())).willReturn(List.of(central1, central2));

        // when
        HubRoutePathResponseDto result = hubRouteService.calculatePath(central1.getId(), central2.getId());

        // then
        assertThat(result.segments()).hasSize(1);
        assertThat(result.segments().get(0).originHub().hubId()).isEqualTo(central1.getId());
        assertThat(result.segments().get(0).destinationHub().hubId()).isEqualTo(central2.getId());
        assertThat(result.originHub().hubId()).isEqualTo(central1.getId());
        assertThat(result.destinationHub().hubId()).isEqualTo(central2.getId());
    }

    // ====== calculatePath 실패 케이스 ======

    @Test
    @DisplayName("출발 허브와 도착 허브가 같으면 예외 발생")
    void calculatePath_sameHub_throwsException() {
        // given
        UUID hubId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> hubRouteService.calculatePath(hubId, hubId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.SAME_HUB_PATH));
        verify(hubRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 출발 허브 경로 계산 시 예외 발생")
    void calculatePath_originNotFound_throwsException() {
        // given
        UUID originId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();
        given(hubRepository.findById(originId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubRouteService.calculatePath(originId, destinationId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("존재하지 않는 도착 허브 경로 계산 시 예외 발생")
    void calculatePath_destinationNotFound_throwsException() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.REGIONAL);
        UUID destinationId = UUID.randomUUID();
        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destinationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubRouteService.calculatePath(origin.getId(), destinationId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("INACTIVE 출발 허브 경로 계산 시 예외 발생")
    void calculatePath_originInactive_throwsException() {
        // given
        Hub origin = buildHub("폐쇄 센터", HubType.REGIONAL);
        origin.startClosing("종료");
        origin.deactivate();
        Hub destination = buildHub("부산광역시 센터", HubType.REGIONAL);
        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));

        // when & then
        assertThatThrownBy(() -> hubRouteService.calculatePath(origin.getId(), destination.getId()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_INACTIVE_IN_PATH));
    }

    @Test
    @DisplayName("CLOSING 출발 허브 경로 계산 시 예외 발생")
    void calculatePath_originClosing_throwsException() {
        // given
        Hub origin = buildHub("마감 예정 센터", HubType.REGIONAL);
        origin.startClosing("마감");
        Hub destination = buildHub("부산광역시 센터", HubType.REGIONAL);
        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));

        // when & then
        assertThatThrownBy(() -> hubRouteService.calculatePath(origin.getId(), destination.getId()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_INACTIVE_IN_PATH));
    }

    @Test
    @DisplayName("INACTIVE 도착 허브 경로 계산 시 예외 발생")
    void calculatePath_destinationInactive_throwsException() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.REGIONAL);
        Hub destination = buildHub("폐쇄 센터", HubType.REGIONAL);
        destination.startClosing("종료");
        destination.deactivate();
        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));

        // when & then
        assertThatThrownBy(() -> hubRouteService.calculatePath(origin.getId(), destination.getId()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_INACTIVE_IN_PATH));
    }

    @Test
    @DisplayName("CLOSING 도착 허브 경로 계산 시 예외 발생")
    void calculatePath_destinationClosing_throwsException() {
        // given
        Hub origin = buildHub("서울특별시 센터", HubType.REGIONAL);
        Hub destination = buildHub("마감 예정 센터", HubType.REGIONAL);
        destination.startClosing("마감");
        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));

        // when & then
        assertThatThrownBy(() -> hubRouteService.calculatePath(origin.getId(), destination.getId()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_INACTIVE_IN_PATH));
    }

    @Test
    @DisplayName("출발 허브에서 중앙 허브로 가는 경로 없을 때 예외 발생")
    void calculatePath_noRouteFromOriginToCentral_throwsException() {
        // given
        Hub central = buildHub("경기남부 센터", HubType.CENTRAL);
        Hub origin = buildHub("서울특별시 센터", HubType.REGIONAL);
        Hub destination = buildHub("인천광역시 센터", HubType.REGIONAL);

        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(origin.getId(), destination.getId()))
                .willReturn(Optional.empty());
        given(hubRepository.findAllByHubType(HubType.CENTRAL)).willReturn(List.of(central));
        given(hubRouteRepository.findAllByOriginHubId(origin.getId()))
                .willReturn(Collections.emptyList()); // CENTRAL로 가는 경로 없음

        // when & then
        assertThatThrownBy(() -> hubRouteService.calculatePath(origin.getId(), destination.getId()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_ROUTE_NOT_FOUND));
    }

    @Test
    @DisplayName("중앙 허브 간 경로 없을 때 예외 발생")
    void calculatePath_noRouteBetweenCentrals_throwsException() {
        // given
        Hub central1 = buildHub("경기남부 센터", HubType.CENTRAL);
        Hub central2 = buildHub("대구광역시 센터", HubType.CENTRAL);
        Hub origin = buildHub("서울특별시 센터", HubType.REGIONAL);
        Hub destination = buildHub("부산광역시 센터", HubType.REGIONAL);
        HubRoute routeOriginToCentral1 = buildSavedRoute(origin.getId(), central1.getId());
        HubRoute routeDestToCentral2 = buildSavedRoute(destination.getId(), central2.getId());

        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(origin.getId(), destination.getId()))
                .willReturn(Optional.empty());
        given(hubRepository.findAllByHubType(HubType.CENTRAL)).willReturn(List.of(central1, central2));
        given(hubRouteRepository.findAllByOriginHubId(origin.getId()))
                .willReturn(List.of(routeOriginToCentral1));
        given(hubRepository.findById(central1.getId())).willReturn(Optional.of(central1));
        given(hubRouteRepository.findAllByOriginHubId(destination.getId()))
                .willReturn(List.of(routeDestToCentral2));
        given(hubRepository.findById(central2.getId())).willReturn(Optional.of(central2));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(origin.getId(), central1.getId()))
                .willReturn(Optional.of(routeOriginToCentral1));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(central1.getId(), central2.getId()))
                .willReturn(Optional.empty()); // 중앙 허브 간 경로 없음

        // when & then
        assertThatThrownBy(() -> hubRouteService.calculatePath(origin.getId(), destination.getId()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_ROUTE_NOT_FOUND));
    }

    @Test
    @DisplayName("도착 중앙 허브에서 도착 허브로 가는 경로 없을 때 예외 발생")
    void calculatePath_noRouteFromCentralToDestination_throwsException() {
        // given
        Hub central1 = buildHub("경기남부 센터", HubType.CENTRAL);
        Hub central2 = buildHub("대구광역시 센터", HubType.CENTRAL);
        Hub origin = buildHub("서울특별시 센터", HubType.REGIONAL);
        Hub destination = buildHub("부산광역시 센터", HubType.REGIONAL);
        HubRoute routeOriginToCentral1 = buildSavedRoute(origin.getId(), central1.getId());
        HubRoute routeDestToCentral2 = buildSavedRoute(destination.getId(), central2.getId());
        HubRoute routeCentral1ToCentral2 = buildSavedRoute(central1.getId(), central2.getId());

        given(hubRepository.findById(origin.getId())).willReturn(Optional.of(origin));
        given(hubRepository.findById(destination.getId())).willReturn(Optional.of(destination));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(origin.getId(), destination.getId()))
                .willReturn(Optional.empty());
        given(hubRepository.findAllByHubType(HubType.CENTRAL)).willReturn(List.of(central1, central2));
        given(hubRouteRepository.findAllByOriginHubId(origin.getId()))
                .willReturn(List.of(routeOriginToCentral1));
        given(hubRepository.findById(central1.getId())).willReturn(Optional.of(central1));
        given(hubRouteRepository.findAllByOriginHubId(destination.getId()))
                .willReturn(List.of(routeDestToCentral2));
        given(hubRepository.findById(central2.getId())).willReturn(Optional.of(central2));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(origin.getId(), central1.getId()))
                .willReturn(Optional.of(routeOriginToCentral1));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(central1.getId(), central2.getId()))
                .willReturn(Optional.of(routeCentral1ToCentral2));
        given(hubRouteRepository.findByOriginHubIdAndDestinationHubId(central2.getId(), destination.getId()))
                .willReturn(Optional.empty()); // 도착 CENTRAL → 도착 허브 경로 없음

        // when & then
        assertThatThrownBy(() -> hubRouteService.calculatePath(origin.getId(), destination.getId()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_ROUTE_NOT_FOUND));
    }
}
