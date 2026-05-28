package com.boxoffice.hubservice.hub.service;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.hub.dto.request.HubClosingRequestDto;
import com.boxoffice.hubservice.hub.dto.request.HubCreateRequestDto;
import com.boxoffice.hubservice.hub.dto.request.HubUpdateRequestDto;
import com.boxoffice.hubservice.hub.dto.response.HubCreateResponseDto;
import com.boxoffice.hubservice.hub.dto.response.HubDeactivateResponseDto;
import com.boxoffice.hubservice.hub.dto.response.HubGetResponseDto;
import com.boxoffice.hubservice.hub.entity.CoordinateVO;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hub.repository.HubRepository;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HubServiceTest {

    @InjectMocks
    private HubService hubService;

    @Mock
    private HubRepository hubRepository;

    private Hub buildHub(String name, HubType hubType) {
        Hub hub = Hub.builder()
                .name(name)
                .address(new AddressVO("12345", "서울시 강남구", "101호"))
                .coordinate(new CoordinateVO(37.5, 127.0))
                .hubType(hubType)
                .build();
        ReflectionTestUtils.setField(hub, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(hub, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(hub, "updatedAt", LocalDateTime.now());
        return hub;
    }

    @Test
    @DisplayName("허브 생성 성공")
    void createHub_success() {
        // given
        HubCreateRequestDto request = new HubCreateRequestDto(
                "서울특별시 센터",
                "05551",
                "서울특별시 송파구 송파대로 55",
                null,
                37.4956,
                127.1236,
                HubType.REGIONAL,
                null
        );

        given(hubRepository.existsByName(request.name())).willReturn(false);
        given(hubRepository.save(any(Hub.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        HubCreateResponseDto response = hubService.createHub(request);

        // then
        assertThat(response.name()).isEqualTo("서울특별시 센터");
        assertThat(response.hubType()).isEqualTo(HubType.REGIONAL);
        assertThat(response.latitude()).isEqualTo(37.4956);
        assertThat(response.createdBy()).isNull();
        verify(hubRepository).save(any(Hub.class));
    }

    @Test
    @DisplayName("CENTRAL 허브 생성 성공")
    void createHub_central_success() {
        // given
        HubCreateRequestDto request = new HubCreateRequestDto(
                "경기 남부 센터",
                null,
                "경기도 이천시 덕평로 257-21",
                null,
                37.2749,
                127.4431,
                HubType.CENTRAL,
                null
        );
        given(hubRepository.existsByName(request.name())).willReturn(false);
        given(hubRepository.save(any(Hub.class))).willAnswer(i -> i.getArgument(0));

        // when
        HubCreateResponseDto response = hubService.createHub(request);

        // then
        assertThat(response.hubType()).isEqualTo(HubType.CENTRAL);
    }

    @Test
    @DisplayName("CLOSING 타입으로 허브 생성 시 예외 발생")
    void createHub_closingType_throwsException() {
        // given
        HubCreateRequestDto request = new HubCreateRequestDto(
                "테스트 센터", null, "서울특별시 송파구 송파대로 55", null,
                37.4956, 127.1236, HubType.CLOSING, null
        );

        // when & then
        assertThatThrownBy(() -> hubService.createHub(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.INVALID_HUB_TYPE));
        verify(hubRepository, never()).existsByName(any());
        verify(hubRepository, never()).save(any());
    }

    @Test
    @DisplayName("INACTIVE 타입으로 허브 생성 시 예외 발생")
    void createHub_inactiveType_throwsException() {
        // given
        HubCreateRequestDto request = new HubCreateRequestDto(
                "테스트 센터",
                null,
                "서울특별시 송파구 송파대로 55",
                null,
                37.4956,
                127.1236,
                HubType.INACTIVE,
                null
        );

        // when & then
        assertThatThrownBy(() -> hubService.createHub(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.INVALID_HUB_TYPE));
        verify(hubRepository, never()).existsByName(any());
        verify(hubRepository, never()).save(any());
    }

    @Test
    @DisplayName("중복된 허브 이름으로 생성 시 예외 발생")
    void createHub_duplicateName_throwsException() {
        // given
        HubCreateRequestDto request = new HubCreateRequestDto(
                "서울특별시 센터",
                null,
                "서울특별시 송파구 송파대로 55",
                null,
                37.4956,
                127.1236,
                HubType.REGIONAL,
                null
        );

        given(hubRepository.existsByName(request.name())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> hubService.createHub(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.DUPLICATE_HUB_NAME));
        verify(hubRepository, never()).save(any());
    }

    @Test
    @DisplayName("허브 단건 조회 성공")
    void getHub_success() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");
        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        // when
        HubGetResponseDto response = hubService.getHub(hubId);

        // then
        assertThat(response.name()).isEqualTo("서울 센터");
        assertThat(response.hubType()).isEqualTo(HubType.REGIONAL);
    }

    @Test
    @DisplayName("존재하지 않는 허브 조회 시 예외 발생")
    void getHub_notFound_throwsException() {
        // given
        UUID hubId = UUID.randomUUID();
        given(hubRepository.findById(hubId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubService.getHub(hubId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("허브 목록 조회 - 필터 없음")
    void getHubs_noFilter_returnsAll() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        Page<Hub> page = new PageImpl<>(List.of(hub));
        given(hubRepository.findAll(any(Predicate.class), any(Pageable.class))).willReturn(page);

        // when
        PageResponse<HubGetResponseDto> response = hubService.getHubs(null, null, 0, 10);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).name()).isEqualTo("서울 센터");
    }

    @Test
    @DisplayName("허브 목록 조회 - 이름 필터 적용")
    void getHubs_withNameFilter_returnsFiltered() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        Page<Hub> page = new PageImpl<>(List.of(hub));
        given(hubRepository.findAll(any(Predicate.class), any(Pageable.class))).willReturn(page);

        // when
        PageResponse<HubGetResponseDto> response = hubService.getHubs("서울", null, 0, 10);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).name()).contains("서울");
    }

    @Test
    @DisplayName("허브 목록 조회 - 허브 타입 필터 적용")
    void getHubs_withHubTypeFilter_returnsFiltered() {
        // given
        Hub hub = buildHub("경기 센터", HubType.CENTRAL);
        Page<Hub> page = new PageImpl<>(List.of(hub));
        given(hubRepository.findAll(any(Predicate.class), any(Pageable.class))).willReturn(page);

        // when
        PageResponse<HubGetResponseDto> response = hubService.getHubs(null, HubType.CENTRAL, 0, 10);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).hubType()).isEqualTo(HubType.CENTRAL);
    }

    @Test
    @DisplayName("허브 목록 조회 - 결과 없음")
    void getHubs_emptyResult_returnsEmptyPage() {
        // given
        Page<Hub> emptyPage = new PageImpl<>(List.of());
        given(hubRepository.findAll(any(Predicate.class), any(Pageable.class))).willReturn(emptyPage);

        // when
        PageResponse<HubGetResponseDto> response = hubService.getHubs("없는허브", HubType.CENTRAL, 0, 10);

        // then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("허브 수정 성공")
    void updateHub_success() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");
        HubUpdateRequestDto request = new HubUpdateRequestDto("서울특별시 센터", null, null, null, null, null, null);

        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));
        given(hubRepository.existsByNameAndIdNot(request.name(), hubId)).willReturn(false);

        // when
        HubGetResponseDto response = hubService.updateHub(hubId, request);

        // then
        assertThat(response.name()).isEqualTo("서울특별시 센터");
    }

    @Test
    @DisplayName("존재하지 않는 허브 수정 시 예외 발생")
    void updateHub_notFound_throwsException() {
        // given
        UUID hubId = UUID.randomUUID();
        HubUpdateRequestDto request = new HubUpdateRequestDto("새 이름", null, null, null, null, null, null);

        given(hubRepository.findById(hubId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubService.updateHub(hubId, request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("INACTIVE 허브 수정 시 예외 발생")
    void updateHub_inactiveHub_throwsException() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        hub.startClosing("마감");
        hub.deactivate();
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");
        HubUpdateRequestDto request = new HubUpdateRequestDto("새 이름", null, null, null, null, null, null);

        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        // when & then
        assertThatThrownBy(() -> hubService.updateHub(hubId, request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_INACTIVE));
    }

    @Test
    @DisplayName("CLOSING 허브 수정 시 예외 발생")
    void updateHub_closingHub_throwsException() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        hub.startClosing("마감 예정");
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");
        HubUpdateRequestDto request = new HubUpdateRequestDto("새 이름", null, null, null, null, null, null);

        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        // when & then
        assertThatThrownBy(() -> hubService.updateHub(hubId, request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_CLOSING));
    }

    @Test
    @DisplayName("중복된 이름으로 허브 수정 시 예외 발생")
    void updateHub_duplicateName_throwsException() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");
        HubUpdateRequestDto request = new HubUpdateRequestDto("부산 센터", null, null, null, null, null, null);

        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));
        given(hubRepository.existsByNameAndIdNot(request.name(), hubId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> hubService.updateHub(hubId, request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.DUPLICATE_HUB_NAME));
    }

    @Test
    @DisplayName("허브 마감 예정 전환 성공")
    void startClosingHub_success() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");
        HubClosingRequestDto request = new HubClosingRequestDto("운영 종료 예정");

        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        // when
        HubGetResponseDto response = hubService.startClosingHub(hubId, request);

        // then
        assertThat(response.hubType()).isEqualTo(HubType.CLOSING);
    }

    @Test
    @DisplayName("CENTRAL 허브 마감 예정 전환 시 예외 발생")
    void startClosingHub_centralHub_throwsException() {
        // given
        Hub hub = buildHub("경기 남부 센터", HubType.CENTRAL);
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");
        HubClosingRequestDto request = new HubClosingRequestDto("운영 종료");

        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        // when & then
        assertThatThrownBy(() -> hubService.startClosingHub(hubId, request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.CENTRAL_HUB_CANNOT_CLOSE));
    }

    @Test
    @DisplayName("이미 CLOSING 허브 마감 예정 전환 시 예외 발생")
    void startClosingHub_alreadyClosing_throwsException() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        hub.startClosing("이미 마감 중");
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");
        HubClosingRequestDto request = new HubClosingRequestDto("재시도");

        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        // when & then
        assertThatThrownBy(() -> hubService.startClosingHub(hubId, request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_ALREADY_CLOSING));
    }

    @Test
    @DisplayName("INACTIVE 허브 마감 예정 전환 시 예외 발생")
    void startClosingHub_alreadyInactive_throwsException() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        hub.startClosing("마감");
        hub.deactivate();
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");
        HubClosingRequestDto request = new HubClosingRequestDto("재시도");

        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        // when & then
        assertThatThrownBy(() -> hubService.startClosingHub(hubId, request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_ALREADY_INACTIVE));
    }

    @Test
    @DisplayName("존재하지 않는 허브 마감 예정 전환 시 예외 발생")
    void startClosingHub_notFound_throwsException() {
        // given
        UUID hubId = UUID.randomUUID();
        HubClosingRequestDto request = new HubClosingRequestDto("운영 종료");

        given(hubRepository.findById(hubId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubService.startClosingHub(hubId, request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("CLOSING 허브 운영 중단 성공")
    void deactivateHub_success() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        hub.startClosing("운영 종료");
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");

        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        // when
        HubDeactivateResponseDto response = hubService.deactivateHub(hubId);

        // then
        assertThat(response.isActive()).isFalse();
        assertThat(hub.isInactive()).isTrue();
    }

    @Test
    @DisplayName("CLOSING 아닌 허브 운영 중단 시 예외 발생")
    void deactivateHub_notClosing_throwsException() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");

        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        // when & then
        assertThatThrownBy(() -> hubService.deactivateHub(hubId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_CLOSING));
    }

    @Test
    @DisplayName("이미 INACTIVE 허브 운영 중단 시 예외 발생")
    void deactivateHub_alreadyInactive_throwsException() {
        // given
        Hub hub = buildHub("서울 센터", HubType.REGIONAL);
        hub.startClosing("마감");
        hub.deactivate();
        UUID hubId = (UUID) ReflectionTestUtils.getField(hub, "id");

        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        // when & then
        assertThatThrownBy(() -> hubService.deactivateHub(hubId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_ALREADY_INACTIVE));
    }

    @Test
    @DisplayName("존재하지 않는 허브 운영 중단 시 예외 발생")
    void deactivateHub_notFound_throwsException() {
        // given
        UUID hubId = UUID.randomUUID();

        given(hubRepository.findById(hubId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hubService.deactivateHub(hubId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

}
