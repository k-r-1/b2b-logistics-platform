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
import boxoffice.deliveryservice.domain.deliveryroute.dto.response.DeliveryRouteResponseDto;
import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRouteStatus;
import boxoffice.deliveryservice.domain.deliveryroute.service.DeliveryRouteService;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    @Nested
    @DisplayName("getDeliveries()")
    class GetDeliveries {

        @Test
        @DisplayName("성공 - 배송 목록 반환")
        void success() {
            // given
            PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Delivery delivery = Delivery.create(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    new AddressRequest("12345", "서울시 송파구 송파대로 55", "101호").toAddressVO(),
                    "홍길동", "U12345"
            );
            Page<Delivery> page = new PageImpl<>(List.of(delivery), pageable, 1);
            given(deliveryRepository.findAllByDeletedAtIsNull(pageable)).willReturn(page);

            // when
            PageResponse<DeliveryResponseDto> result = deliveryService.getDeliveries(pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).orderId()).isEqualTo(delivery.getOrderId());
            assertThat(result.getContent().get(0).deliveryStatus()).isEqualTo(DeliveryStatus.WAITING);
        }

        @Test
        @DisplayName("성공 - 빈 목록 반환")
        void success_empty() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Delivery> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            given(deliveryRepository.findAllByDeletedAtIsNull(pageable)).willReturn(emptyPage);

            // when
            PageResponse<DeliveryResponseDto> result = deliveryService.getDeliveries(pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("getDelivery()")
    class GetDelivery {

        @Test
        @DisplayName("성공 - 배송 단건 반환")
        void success() {
            // given
            UUID deliveryId = UUID.randomUUID();
            Delivery delivery = Delivery.create(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    new AddressRequest("12345", "서울시 송파구 송파대로 55", "101호").toAddressVO(),
                    "홍길동", "U12345"
            );
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            DeliveryResponseDto result = deliveryService.getDelivery(deliveryId);

            // then
            assertThat(result.orderId()).isEqualTo(delivery.getOrderId());
            assertThat(result.recipientName()).isEqualTo("홍길동");
            assertThat(result.deliveryStatus()).isEqualTo(DeliveryStatus.WAITING);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 배송 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("getDeliveryRoutes()")
    class GetDeliveryRoutes {

        @Test
        @DisplayName("성공 - 배송 경로 목록 조회를 DeliveryRouteService에 위임")
        void success() {
            // given
            UUID deliveryId = UUID.randomUUID();
            PageRequest pageable = PageRequest.of(0, 10, Sort.by("sequence").ascending());
            Delivery delivery = Delivery.create(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    new AddressRequest("12345", "서울시 송파구 송파대로 55", "101호").toAddressVO(),
                    "홍길동", "U12345"
            );
            Page<DeliveryRouteResponseDto> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            PageResponse<DeliveryRouteResponseDto> mockResponse = PageResponse.of(emptyPage);

            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
            given(deliveryRouteService.getRoutesByDelivery(deliveryId, pageable)).willReturn(mockResponse);

            // when
            PageResponse<DeliveryRouteResponseDto> result = deliveryService.getDeliveryRoutes(deliveryId, pageable);

            // then
            assertThat(result).isEqualTo(mockResponse);
            verify(deliveryRouteService).getRoutesByDelivery(deliveryId, pageable);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 배송 ID면 경로 서비스 호출 안 함")
        void fail_delivery_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            PageRequest pageable = PageRequest.of(0, 10);
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.getDeliveryRoutes(deliveryId, pageable))
                    .isInstanceOf(BaseException.class);

            verify(deliveryRouteService, never()).getRoutesByDelivery(any(), any());
        }
    }

    @Nested
    @DisplayName("getDeliveryRoute()")
    class GetDeliveryRoute {

        @Test
        @DisplayName("성공 - 배송 경로 단건 조회를 DeliveryRouteService에 위임")
        void success() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            Delivery delivery = Delivery.create(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    new AddressRequest("12345", "서울시 송파구 송파대로 55", "101호").toAddressVO(),
                    "홍길동", "U12345"
            );
            DeliveryRouteResponseDto mockRouteDto = new DeliveryRouteResponseDto(
                    routeId, deliveryId, UUID.randomUUID(), UUID.randomUUID(),
                    null, DeliveryRouteStatus.WAITING, 1,
                    new BigDecimal("100.5"), 60, null, null, LocalDateTime.now()
            );

            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
            given(deliveryRouteService.getRouteByDelivery(deliveryId, routeId)).willReturn(mockRouteDto);

            // when
            DeliveryRouteResponseDto result = deliveryService.getDeliveryRoute(deliveryId, routeId);

            // then
            assertThat(result).isEqualTo(mockRouteDto);
            verify(deliveryRouteService).getRouteByDelivery(deliveryId, routeId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 배송 ID면 경로 서비스 호출 안 함")
        void fail_delivery_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.getDeliveryRoute(deliveryId, routeId))
                    .isInstanceOf(BaseException.class);

            verify(deliveryRouteService, never()).getRouteByDelivery(any(), any());
        }
    }
}
