package boxoffice.deliveryservice.domain.delivery.service;

import boxoffice.deliveryservice.client.HubClient;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto.HubInfo;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto.HubRouteSegmentDto;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto.HubType;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto.AddressRequest;
import boxoffice.deliveryservice.domain.delivery.dto.response.DeliveryResponseDto;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.delivery.entity.DeliveryStatus;
import boxoffice.deliveryservice.domain.delivery.repository.DeliveryRepository;
import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRoute;
import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRouteStatus;
import boxoffice.deliveryservice.domain.deliveryroute.repository.DeliveryRouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@DisplayName("DeliveryService 테스트")
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @InjectMocks
    private DeliveryService deliveryService;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryRouteRepository deliveryRouteRepository;

    @Mock
    private HubClient hubClient;

    @Captor
    private ArgumentCaptor<List<DeliveryRoute>> routesCaptor;

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
        @DisplayName("성공 - 배송 및 경로 2구간 생성")
        void success() {
            // given
            UUID hubId1 = UUID.randomUUID();
            UUID hubId2 = UUID.randomUUID();
            UUID hubId3 = UUID.randomUUID();

            HubInfo seoulHub = new HubInfo(hubId1, "서울 허브", HubType.REGIONAL);
            HubInfo daejeonHub = new HubInfo(hubId2, "대전 허브", HubType.CENTRAL);
            HubInfo busanHub = new HubInfo(hubId3, "부산 허브", HubType.REGIONAL);

            HubRouteResponseDto hubRoute = new HubRouteResponseDto(
                    seoulHub,
                    busanHub,
                    List.of(
                            new HubRouteSegmentDto(1, seoulHub, daejeonHub, 120, new BigDecimal("160.5")),
                            new HubRouteSegmentDto(2, daejeonHub, busanHub, 90, new BigDecimal("120.3"))
                    ),
                    210,
                    new BigDecimal("280.8")
            );

            given(deliveryRepository.save(any(Delivery.class))).willAnswer(inv -> inv.getArgument(0));
            given(hubClient.calculatePath(originHubId, destinationHubId)).willReturn(hubRoute);
            given(deliveryRouteRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            // when
            DeliveryResponseDto result = deliveryService.createDelivery(request);

            // then
            assertThat(result.orderId()).isEqualTo(orderId);
            assertThat(result.originHubId()).isEqualTo(originHubId);
            assertThat(result.destinationHubId()).isEqualTo(destinationHubId);
            assertThat(result.deliveryStatus()).isEqualTo(DeliveryStatus.WAITING);
            assertThat(result.deliveryPersonId()).isNull();

            verify(deliveryRouteRepository).saveAll(routesCaptor.capture());
            List<DeliveryRoute> savedRoutes = routesCaptor.getValue();

            assertThat(savedRoutes).hasSize(2);
            assertThat(savedRoutes.get(0).getSequence()).isEqualTo(1);
            assertThat(savedRoutes.get(0).getOriginHubId()).isEqualTo(hubId1);
            assertThat(savedRoutes.get(0).getDestinationHubId()).isEqualTo(hubId2);
            assertThat(savedRoutes.get(0).getExpectedDuration()).isEqualTo(120);
            assertThat(savedRoutes.get(0).getExpectedDistance()).isEqualByComparingTo(new BigDecimal("160.5"));
            assertThat(savedRoutes.get(0).getStatus()).isEqualTo(DeliveryRouteStatus.WAITING);
            assertThat(savedRoutes.get(1).getSequence()).isEqualTo(2);
            assertThat(savedRoutes.get(1).getOriginHubId()).isEqualTo(hubId2);
            assertThat(savedRoutes.get(1).getDestinationHubId()).isEqualTo(hubId3);
        }

        @Test
        @DisplayName("실패 - 배송 저장 중 DB 예외 발생 시 전파")
        void fail_when_delivery_save_throws() {
            // given
            given(deliveryRepository.save(any(Delivery.class)))
                    .willThrow(new RuntimeException("DB 저장 실패"));

            // when & then
            assertThatThrownBy(() -> deliveryService.createDelivery(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 저장 실패");

            verify(hubClient, never()).calculatePath(any(), any());
            verify(deliveryRouteRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("실패 - hub-service 호출 중 예외 발생 시 전파")
        void fail_when_hub_client_throws() {
            // given
            given(deliveryRepository.save(any(Delivery.class))).willAnswer(inv -> inv.getArgument(0));
            given(hubClient.calculatePath(originHubId, destinationHubId))
                    .willThrow(new RuntimeException("hub-service 호출 실패"));

            // when & then
            assertThatThrownBy(() -> deliveryService.createDelivery(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("hub-service 호출 실패");

            verify(deliveryRouteRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("실패 - 배송 경로 저장 중 예외 발생 시 전파")
        void fail_when_route_save_throws() {
            // given
            UUID hubId1 = UUID.randomUUID();
            UUID hubId2 = UUID.randomUUID();
            HubInfo originHubInfo = new HubInfo(hubId1, "서울 허브", HubType.REGIONAL);
            HubInfo destHubInfo = new HubInfo(hubId2, "부산 허브", HubType.REGIONAL);
            HubRouteResponseDto hubRoute = new HubRouteResponseDto(
                    originHubInfo, destHubInfo,
                    List.of(new HubRouteSegmentDto(1, originHubInfo, destHubInfo, 300, new BigDecimal("450.0"))),
                    300, new BigDecimal("450.0")
            );

            given(deliveryRepository.save(any(Delivery.class))).willAnswer(inv -> inv.getArgument(0));
            given(hubClient.calculatePath(originHubId, destinationHubId)).willReturn(hubRoute);
            given(deliveryRouteRepository.saveAll(anyList()))
                    .willThrow(new RuntimeException("배송 경로 저장 실패"));

            // when & then
            assertThatThrownBy(() -> deliveryService.createDelivery(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("배송 경로 저장 실패");
        }

        @Test
        @DisplayName("성공 - hub-service fallback 시 배송만 생성되고 경로는 빈 리스트")
        void success_when_hub_fallback() {
            // given
            HubRouteResponseDto emptyRoute = new HubRouteResponseDto(
                    null, null, List.of(), 0, BigDecimal.ZERO
            );

            given(deliveryRepository.save(any(Delivery.class))).willAnswer(inv -> inv.getArgument(0));
            given(hubClient.calculatePath(originHubId, destinationHubId)).willReturn(emptyRoute);
            given(deliveryRouteRepository.saveAll(anyList())).willReturn(List.of());

            // when
            DeliveryResponseDto result = deliveryService.createDelivery(request);

            // then
            assertThat(result.orderId()).isEqualTo(orderId);
            assertThat(result.deliveryStatus()).isEqualTo(DeliveryStatus.WAITING);

            verify(deliveryRouteRepository).saveAll(routesCaptor.capture());
            assertThat(routesCaptor.getValue()).isEmpty();
        }
    }
}