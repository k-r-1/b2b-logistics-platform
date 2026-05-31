package com.boxoffice.companyservice.company.validator;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.companyservice.company.client.HubClient;
import com.boxoffice.companyservice.company.client.dto.HubActiveResponseDto;
import com.boxoffice.companyservice.company.exception.CompanyErrorCode;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HubValidator 테스트")
class HubValidatorTest {

    @InjectMocks
    private HubValidator hubValidator;

    @Mock
    private HubClient hubClient;

    @Test
    @DisplayName("성공 - Hub가 활성 상태이면 예외 없이 통과한다")
    void validateHubActiveWithActiveHub() {
        // given
        UUID hubId = UUID.randomUUID();
        when(hubClient.checkHubActive(hubId)).thenReturn(createWrapper(hubId, true));

        // when
        Throwable throwable = catchThrowable(() -> hubValidator.validateHubActive(hubId));

        // then
        assertThat(throwable).isNull();
        verify(hubClient).checkHubActive(hubId);
        verifyNoMoreInteractions(hubClient);
    }

    @Test
    @DisplayName("실패 - Hub가 비활성 상태이면 HUB_INACTIVE 예외로 변환한다")
    void validateHubActiveWithInactiveHub() {
        // given
        UUID hubId = UUID.randomUUID();
        when(hubClient.checkHubActive(hubId)).thenReturn(createWrapper(hubId, false));

        // when
        Throwable throwable = catchThrowable(() -> hubValidator.validateHubActive(hubId));

        // then
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CompanyErrorCode.HUB_INACTIVE));
        verify(hubClient).checkHubActive(hubId);
        verifyNoMoreInteractions(hubClient);
    }

    @Test
    @DisplayName("실패 - Hub Service가 404를 반환하면 HUB_NOT_FOUND 예외로 변환한다")
    void validateHubActiveWithNotFoundHub() {
        // given
        UUID hubId = UUID.randomUUID();
        when(hubClient.checkHubActive(hubId)).thenThrow(createFeignException(404));

        // when
        Throwable throwable = catchThrowable(() -> hubValidator.validateHubActive(hubId));

        // then
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CompanyErrorCode.HUB_NOT_FOUND));
        verify(hubClient).checkHubActive(hubId);
        verifyNoMoreInteractions(hubClient);
    }

    @Test
    @DisplayName("실패 - Hub Service 기타 오류는 FEIGN_CLIENT_ERROR 예외로 변환한다")
    void validateHubActiveWithFeignError() {
        // given
        UUID hubId = UUID.randomUUID();
        when(hubClient.checkHubActive(hubId)).thenThrow(createFeignException(500));

        // when
        Throwable throwable = catchThrowable(() -> hubValidator.validateHubActive(hubId));

        // then
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FEIGN_CLIENT_ERROR));
        verify(hubClient).checkHubActive(hubId);
        verifyNoMoreInteractions(hubClient);
    }

    @Test
    @DisplayName("실패 - Hub 응답 wrapper가 null이면 FEIGN_CLIENT_ERROR 예외로 변환한다")
    void validateHubActiveWithNullWrapper() {
        // given
        UUID hubId = UUID.randomUUID();
        when(hubClient.checkHubActive(hubId)).thenReturn(null);

        // when
        Throwable throwable = catchThrowable(() -> hubValidator.validateHubActive(hubId));

        // then
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FEIGN_CLIENT_ERROR));
        verify(hubClient).checkHubActive(hubId);
        verifyNoMoreInteractions(hubClient);
    }

    @Test
    @DisplayName("실패 - Hub 응답 data가 null이면 FEIGN_CLIENT_ERROR 예외로 변환한다")
    void validateHubActiveWithNullData() {
        // given
        UUID hubId = UUID.randomUUID();
        when(hubClient.checkHubActive(hubId)).thenReturn(createWrapperWithoutData());

        // when
        Throwable throwable = catchThrowable(() -> hubValidator.validateHubActive(hubId));

        // then
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FEIGN_CLIENT_ERROR));
        verify(hubClient).checkHubActive(hubId);
        verifyNoMoreInteractions(hubClient);
    }

    // Hub Service의 정상 wrapper 응답을 테스트용으로 구성한다.
    private ApiResponse<HubActiveResponseDto> createWrapper(UUID hubId, boolean active) {
        HubActiveResponseDto data = new HubActiveResponseDto();
        ReflectionTestUtils.setField(data, "hubId", hubId);
        ReflectionTestUtils.setField(data, "active", active);

        return ApiResponse.success(data);
    }

    // wrapper는 있지만 data가 빠진 비정상 응답을 구성한다.
    private ApiResponse<HubActiveResponseDto> createWrapperWithoutData() {
        return ApiResponse.success(null);
    }

    // FeignClient가 던지는 HTTP 상태별 예외를 테스트용으로 만든다.
    private FeignException createFeignException(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/v1/internal/hubs/{hubId}/active",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        return FeignException.errorStatus(
                "HubClient#checkHubActive(UUID)",
                feign.Response.builder()
                        .status(status)
                        .reason("Feign error")
                        .request(request)
                        .build()
        );
    }
}
