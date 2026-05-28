package boxoffice.deliveryservice.domain.delivery.service;

import boxoffice.deliveryservice.client.HubClient;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto;
import com.boxoffice.common.response.ApiResponse;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto.HubInfo;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto.HubRouteSegmentDto;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto.HubType;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto.AddressRequest;
import boxoffice.deliveryservice.domain.delivery.dto.response.DeliveryResponseDto;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.delivery.entity.DeliveryStatus;
import boxoffice.deliveryservice.domain.delivery.repository.DeliveryRepository;
import boxoffice.deliveryservice.domain.deliveryroute.service.DeliveryRouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("DeliveryService 테스트")
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @InjectMocks
    private DeliveryService deliveryService;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryRouteService deliveryRouteService;

    @Mock
    private HubClient hubClient;

    @Nested
    @DisplayName("createDelivery()")
    class CreateDelivery {

        private UUID orderId;
        private UUID originHubId;
        private UUID destinationHubId;
        private DeliveryCreateRequestDto request;

        @BeforeEach
        void setUp() {
            orderId = UUID.randomUUID();
            originHubId = UUID.randomUUID();
            destinationHubId = UUID.randomUUID();
            request = new DeliveryCreateRequestDto(
                    orderId,
                    originHubId,
                    destinationHubId,
                    new AddressRequest("12345", "서울시 송파구 송파대로 55", "101호"),
                    "홍길동",
                    "U12345"
            );
        }

        @Test
        @DisplayName("성공 - 배송 생성 및 경로 생성 위임")
        void success() {
            // given
            HubInfo seoulHub = new HubInfo(UUID.randomUUID(), "서울 허브", HubType.REGIONAL);
            HubInfo busanHub = new HubInfo(UUID.randomUUID(), "부산 허브", HubType.REGIONAL);
            List<HubRouteSegmentDto> segments = List.of(
                    new HubRouteSegmentDto(1, seoulHub, busanHub, 210, new BigDecimal("280.8"))
            );
            HubRouteResponseDto hubRoute = new HubRouteResponseDto(seoulHub, busanHub, segments, 210, new BigDecimal("280.8"));

            given(deliveryRepository.save(any(Delivery.class))).willAnswer(inv -> inv.getArgument(0));
            given(hubClient.calculatePath(originHubId, destinationHubId)).willReturn(ApiResponse.success(hubRoute));

            // when
            DeliveryResponseDto result = deliveryService.createDelivery(request);

            // then
            assertThat(result.orderId()).isEqualTo(orderId);
            assertThat(result.originHubId()).isEqualTo(originHubId);
            assertThat(result.destinationHubId()).isEqualTo(destinationHubId);
            assertThat(result.deliveryStatus()).isEqualTo(DeliveryStatus.WAITING);
            assertThat(result.deliveryPersonId()).isNull();
            verify(deliveryRouteService).createRoutes(any(Delivery.class), eq(segments));
        }

        @Test
        @DisplayName("성공 - hub-service fallback 시 빈 segments로 경로 생성 위임")
        void success_when_hub_fallback() {
            // given
            HubRouteResponseDto emptyRoute = new HubRouteResponseDto(null, null, List.of(), 0, BigDecimal.ZERO);

            given(deliveryRepository.save(any(Delivery.class))).willAnswer(inv -> inv.getArgument(0));
            given(hubClient.calculatePath(originHubId, destinationHubId)).willReturn(ApiResponse.success(emptyRoute));

            // when
            DeliveryResponseDto result = deliveryService.createDelivery(request);

            // then
            assertThat(result.deliveryStatus()).isEqualTo(DeliveryStatus.WAITING);
            verify(deliveryRouteService).createRoutes(any(Delivery.class), eq(List.of()));
        }

        @Test
        @DisplayName("실패 - DB 저장 실패 시 이후 단계 실행 안 함")
        void fail_when_delivery_save_throws() {
            // given
            given(deliveryRepository.save(any(Delivery.class)))
                    .willThrow(new RuntimeException("DB 저장 실패"));

            // when & then
            assertThatThrownBy(() -> deliveryService.createDelivery(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 저장 실패");

            verify(hubClient, never()).calculatePath(any(), any());
            verify(deliveryRouteService, never()).createRoutes(any(), any());
        }

        @Test
        @DisplayName("실패 - hub-service 예외 발생 시 경로 생성 실행 안 함")
        void fail_when_hub_client_throws() {
            // given
            given(deliveryRepository.save(any(Delivery.class))).willAnswer(inv -> inv.getArgument(0));
            given(hubClient.calculatePath(originHubId, destinationHubId))
                    .willThrow(new RuntimeException("hub-service 호출 실패"));


            // when & then
            assertThatThrownBy(() -> deliveryService.createDelivery(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("hub-service 호출 실패");

            verify(deliveryRouteService, never()).createRoutes(any(), any());
        }

        @Test
        @DisplayName("실패 - 경로 생성 예외 발생 시 전파")
        void fail_when_route_create_throws() {
            // given
            HubRouteResponseDto hubRoute = new HubRouteResponseDto(null, null, List.of(), 0, BigDecimal.ZERO);

            given(deliveryRepository.save(any(Delivery.class))).willAnswer(inv -> inv.getArgument(0));
            given(hubClient.calculatePath(originHubId, destinationHubId)).willReturn(ApiResponse.success(hubRoute));
            willThrow(new RuntimeException("경로 저장 실패"))
                    .given(deliveryRouteService).createRoutes(any(), any());

            // when & then
            assertThatThrownBy(() -> deliveryService.createDelivery(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("경로 저장 실패");
        }
    }
}