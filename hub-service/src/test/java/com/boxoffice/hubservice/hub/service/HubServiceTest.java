package com.boxoffice.hubservice.hub.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.hub.dto.request.HubCreateRequestDto;
import com.boxoffice.hubservice.hub.dto.response.HubCreateResponseDto;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hub.repository.HubRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

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
                HubType.REGIONAL
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
                HubType.CENTRAL
        );
        given(hubRepository.existsByName(request.name())).willReturn(false);
        given(hubRepository.save(any(Hub.class))).willAnswer(i -> i.getArgument(0));

        // when
        HubCreateResponseDto response = hubService.createHub(request);

        // then
        assertThat(response.hubType()).isEqualTo(HubType.CENTRAL);
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
                HubType.INACTIVE
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
                HubType.REGIONAL
        );

        given(hubRepository.existsByName(request.name())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> hubService.createHub(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(HubErrorCode.DUPLICATE_HUB_NAME));
        verify(hubRepository, never()).save(any());
    }
}
