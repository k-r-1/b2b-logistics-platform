package boxoffice.deliveryservice.domain.delivery.service;

import boxoffice.deliveryservice.client.HubClient;
import boxoffice.deliveryservice.client.UserServiceClient;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto.HubInfo;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto.HubRouteSegmentDto;
import boxoffice.deliveryservice.client.dto.response.HubRouteResponseDto.HubType;
import boxoffice.deliveryservice.client.dto.response.UserResponseDto;
import boxoffice.deliveryservice.client.entity.UserRole;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryStatusUpdateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryUpdateRequestDto;
import boxoffice.deliveryservice.domain.delivery.dto.response.DeliveryResponseDto;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.delivery.entity.DeliveryStatus;
import boxoffice.deliveryservice.domain.delivery.repository.DeliveryRepository;
import boxoffice.deliveryservice.domain.deliveryroute.dto.request.DeliveryRouteStatusUpdateRequestDto;
import boxoffice.deliveryservice.domain.deliveryroute.dto.request.DeliveryRouteUpdateRequestDto;
import boxoffice.deliveryservice.domain.deliveryroute.dto.response.DeliveryRouteResponseDto;
import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRouteStatus;
import boxoffice.deliveryservice.domain.deliveryroute.service.DeliveryRouteService;
import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.ApiResponse;
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

    @Mock
    private UserServiceClient userServiceClient;

    private Delivery createDelivery(UUID companyId) {
        return Delivery.create(
                UUID.randomUUID(), companyId, UUID.randomUUID(), UUID.randomUUID(),
                new AddressVO("12345", "서울시 송파구 송파대로 55", "101호"),
                "홍길동", "U12345"
        );
    }

    @Nested
    @DisplayName("createDelivery()")
    class CreateDelivery {

        private UUID orderId;
        private UUID companyId;
        private UUID originHubId;
        private UUID destinationHubId;
        private DeliveryCreateRequestDto request;

        @BeforeEach
        void setUp() {
            orderId = UUID.randomUUID();
            companyId = UUID.randomUUID();
            originHubId = UUID.randomUUID();
            destinationHubId = UUID.randomUUID();
            request = new DeliveryCreateRequestDto(
                    orderId,
                    companyId,
                    originHubId,
                    destinationHubId,
                    new AddressVO("12345", "서울시 송파구 송파대로 55", "101호"),
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
            assertThat(result.companyId()).isEqualTo(companyId);
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

        private final String keycloakSub = "sub-" + UUID.randomUUID();
        private final PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        @Test
        @DisplayName("성공 - MASTER는 전체 목록 조회")
        void success_master() {
            // given
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());
            Page<Delivery> page = new PageImpl<>(List.of(delivery), pageable, 1);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findAllByDeletedAtIsNull(pageable)).willReturn(page);

            // when
            PageResponse<DeliveryResponseDto> result = deliveryService.getDeliveries(keycloakSub, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(deliveryRepository).findAllByDeletedAtIsNull(pageable);
        }

        @Test
        @DisplayName("성공 - HUB_MANAGER는 소속 허브 배송만 조회")
        void success_hub_manager() {
            // given
            UUID hubId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.HUB_MANAGER).hubId(hubId).build();
            Page<Delivery> page = new PageImpl<>(List.of(), pageable, 0);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findAllByHubIdAndDeletedAtIsNull(hubId, pageable)).willReturn(page);

            // when
            PageResponse<DeliveryResponseDto> result = deliveryService.getDeliveries(keycloakSub, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            verify(deliveryRepository).findAllByHubIdAndDeletedAtIsNull(hubId, pageable);
        }

        @Test
        @DisplayName("성공 - DELIVERY_MANAGER는 본인 담당 배송만 조회")
        void success_delivery_manager() {
            // given
            UUID userId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(userId).role(UserRole.DELIVERY_MANAGER).build();
            Page<Delivery> page = new PageImpl<>(List.of(), pageable, 0);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findAllByDeliveryPersonIdAndDeletedAtIsNull(userId, pageable)).willReturn(page);

            // when
            deliveryService.getDeliveries(keycloakSub, pageable);

            // then
            verify(deliveryRepository).findAllByDeliveryPersonIdAndDeletedAtIsNull(userId, pageable);
        }

        @Test
        @DisplayName("성공 - SUPPLIER_MANAGER는 본인 업체 배송만 조회")
        void success_supplier_manager() {
            // given
            UUID companyId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.SUPPLIER_MANAGER).companyId(companyId).build();
            Page<Delivery> page = new PageImpl<>(List.of(), pageable, 0);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findAllByCompanyIdAndDeletedAtIsNull(companyId, pageable)).willReturn(page);

            // when
            deliveryService.getDeliveries(keycloakSub, pageable);

            // then
            verify(deliveryRepository).findAllByCompanyIdAndDeletedAtIsNull(companyId, pageable);
        }
    }

    @Nested
    @DisplayName("getDelivery()")
    class GetDelivery {

        private final String keycloakSub = "sub-" + UUID.randomUUID();

        @Test
        @DisplayName("성공 - MASTER는 모든 배송 조회 가능")
        void success_master() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();
            Delivery delivery = createDelivery(companyId);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            DeliveryResponseDto result = deliveryService.getDelivery(keycloakSub, deliveryId);

            // then
            assertThat(result.orderId()).isEqualTo(delivery.getOrderId());
        }

        @Test
        @DisplayName("성공 - HUB_MANAGER는 소속 허브의 배송 조회 가능")
        void success_hub_manager() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.HUB_MANAGER).hubId(hubId).build();
            Delivery delivery = Delivery.create(
                    UUID.randomUUID(), UUID.randomUUID(), hubId, UUID.randomUUID(),
                    new AddressVO("12345", "서울시 송파구 송파대로 55", "101호"),
                    "홍길동", "U12345"
            );

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            DeliveryResponseDto result = deliveryService.getDelivery(keycloakSub, deliveryId);

            // then
            assertThat(result.originHubId()).isEqualTo(hubId);
        }

        @Test
        @DisplayName("실패 - HUB_MANAGER가 다른 허브 배송 조회 시 FORBIDDEN")
        void fail_hub_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.HUB_MANAGER).hubId(UUID.randomUUID()).build();
            Delivery delivery = createDelivery(UUID.randomUUID());

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.getDelivery(keycloakSub, deliveryId))
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("성공 - DELIVERY_MANAGER는 본인 담당 배송 조회 가능")
        void success_delivery_manager() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(userId).role(UserRole.DELIVERY_MANAGER).build();
            Delivery delivery = Delivery.create(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    new AddressVO("12345", "서울시 송파구 송파대로 55", "101호"),
                    "홍길동", "U12345"
            );
            delivery.assignDeliveryPerson(userId);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            DeliveryResponseDto result = deliveryService.getDelivery(keycloakSub, deliveryId);

            // then
            assertThat(result.deliveryPersonId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("실패 - DELIVERY_MANAGER가 다른 사람 담당 배송 조회 시 FORBIDDEN")
        void fail_delivery_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.DELIVERY_MANAGER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.getDelivery(keycloakSub, deliveryId))
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("성공 - SUPPLIER_MANAGER는 본인 업체 배송 조회 가능")
        void success_supplier_manager() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.SUPPLIER_MANAGER).companyId(companyId).build();
            Delivery delivery = createDelivery(companyId);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            DeliveryResponseDto result = deliveryService.getDelivery(keycloakSub, deliveryId);

            // then
            assertThat(result.companyId()).isEqualTo(companyId);
        }

        @Test
        @DisplayName("실패 - SUPPLIER_MANAGER가 다른 업체 배송 조회 시 FORBIDDEN")
        void fail_supplier_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.SUPPLIER_MANAGER).companyId(UUID.randomUUID()).build();
            Delivery delivery = createDelivery(UUID.randomUUID());

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.getDelivery(keycloakSub, deliveryId))
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 배송 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.getDelivery(keycloakSub, deliveryId))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("getDeliveryRoutes()")
    class GetDeliveryRoutes {

        private final String keycloakSub = "sub-" + UUID.randomUUID();

        @Test
        @DisplayName("성공 - 접근 권한 있는 배송의 경로 목록 조회를 DeliveryRouteService에 위임")
        void success() {
            // given
            UUID deliveryId = UUID.randomUUID();
            PageRequest pageable = PageRequest.of(0, 10, Sort.by("sequence").ascending());
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());
            Page<DeliveryRouteResponseDto> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            PageResponse<DeliveryRouteResponseDto> mockResponse = PageResponse.of(emptyPage);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
            given(deliveryRouteService.getRoutesByDelivery(deliveryId, pageable)).willReturn(mockResponse);

            // when
            PageResponse<DeliveryRouteResponseDto> result = deliveryService.getDeliveryRoutes(keycloakSub, deliveryId, pageable);

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
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.getDeliveryRoutes(keycloakSub, deliveryId, pageable))
                    .isInstanceOf(BaseException.class);

            verify(deliveryRouteService, never()).getRoutesByDelivery(any(), any());
        }

        @Test
        @DisplayName("실패 - 접근 권한 없는 배송의 경로 조회 시 FORBIDDEN")
        void fail_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            PageRequest pageable = PageRequest.of(0, 10);
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.SUPPLIER_MANAGER).companyId(UUID.randomUUID()).build();
            Delivery delivery = createDelivery(UUID.randomUUID());

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.getDeliveryRoutes(keycloakSub, deliveryId, pageable))
                    .isInstanceOf(BaseException.class);

            verify(deliveryRouteService, never()).getRoutesByDelivery(any(), any());
        }
    }

    @Nested
    @DisplayName("getDeliveryRoute()")
    class GetDeliveryRoute {

        private final String keycloakSub = "sub-" + UUID.randomUUID();

        @Test
        @DisplayName("성공 - 접근 권한 있는 배송의 경로 단건 조회를 DeliveryRouteService에 위임")
        void success() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());
            DeliveryRouteResponseDto mockRouteDto = new DeliveryRouteResponseDto(
                    routeId, deliveryId, UUID.randomUUID(), UUID.randomUUID(),
                    null, DeliveryRouteStatus.WAITING, 1,
                    new BigDecimal("100.5"), 60, null, null, LocalDateTime.now()
            );

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
            given(deliveryRouteService.getRouteByDelivery(deliveryId, routeId)).willReturn(mockRouteDto);

            // when
            DeliveryRouteResponseDto result = deliveryService.getDeliveryRoute(keycloakSub, deliveryId, routeId);

            // then
            assertThat(result).isEqualTo(mockRouteDto);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 배송 ID면 경로 서비스 호출 안 함")
        void fail_delivery_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.getDeliveryRoute(keycloakSub, deliveryId, routeId))
                    .isInstanceOf(BaseException.class);

            verify(deliveryRouteService, never()).getRouteByDelivery(any(), any());
        }

        @Test
        @DisplayName("실패 - 접근 권한 없는 배송의 경로 단건 조회 시 FORBIDDEN")
        void fail_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.SUPPLIER_MANAGER).companyId(UUID.randomUUID()).build();
            Delivery delivery = createDelivery(UUID.randomUUID());

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.getDeliveryRoute(keycloakSub, deliveryId, routeId))
                    .isInstanceOf(BaseException.class);

            verify(deliveryRouteService, never()).getRouteByDelivery(any(), any());
        }
    }

    @Nested
    @DisplayName("updateDelivery()")
    class UpdateDelivery {

        private final String keycloakSub = "sub-" + UUID.randomUUID();

        private DeliveryUpdateRequestDto buildRequest() {
            return new DeliveryUpdateRequestDto(
                    "이순신",
                    "U99999",
                    new DeliveryUpdateRequestDto.AddressRequest("11111", "서울시 강남구", "201호")
            );
        }

        @Test
        @DisplayName("성공 - MASTER는 모든 배송 수정 가능")
        void success_master() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            DeliveryResponseDto result = deliveryService.updateDelivery(keycloakSub, deliveryId, buildRequest());

            // then
            assertThat(result.recipientName()).isEqualTo("이순신");
        }

        @Test
        @DisplayName("성공 - HUB_MANAGER는 소속 허브의 배송 수정 가능")
        void success_hub_manager() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.HUB_MANAGER).hubId(hubId).build();
            Delivery delivery = Delivery.create(
                    UUID.randomUUID(), UUID.randomUUID(), hubId, UUID.randomUUID(),
                    new AddressVO("12345", "서울시 송파구 송파대로 55", "101호"),
                    "홍길동", "U12345"
            );

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            DeliveryResponseDto result = deliveryService.updateDelivery(keycloakSub, deliveryId, buildRequest());

            // then
            assertThat(result.recipientName()).isEqualTo("이순신");
        }

        @Test
        @DisplayName("실패 - HUB_MANAGER가 다른 허브 배송 수정 시 FORBIDDEN")
        void fail_hub_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.HUB_MANAGER).hubId(UUID.randomUUID()).build();
            Delivery delivery = createDelivery(UUID.randomUUID());

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.updateDelivery(keycloakSub, deliveryId, buildRequest()))
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("성공 - DELIVERY_MANAGER는 본인 담당 배송 수정 가능")
        void success_delivery_manager() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(userId).role(UserRole.DELIVERY_MANAGER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());
            delivery.assignDeliveryPerson(userId);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            DeliveryResponseDto result = deliveryService.updateDelivery(keycloakSub, deliveryId, buildRequest());

            // then
            assertThat(result.recipientName()).isEqualTo("이순신");
        }

        @Test
        @DisplayName("실패 - DELIVERY_MANAGER가 다른 사람 담당 배송 수정 시 FORBIDDEN")
        void fail_delivery_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.DELIVERY_MANAGER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.updateDelivery(keycloakSub, deliveryId, buildRequest()))
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("실패 - SUPPLIER_MANAGER는 항상 FORBIDDEN")
        void fail_supplier_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.SUPPLIER_MANAGER).companyId(companyId).build();
            Delivery delivery = createDelivery(companyId);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.updateDelivery(keycloakSub, deliveryId, buildRequest()))
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 배송 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.updateDelivery(keycloakSub, deliveryId, buildRequest()))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("updateDeliveryStatus()")
    class UpdateDeliveryStatus {

        private final String keycloakSub = "sub-" + UUID.randomUUID();

        @Test
        @DisplayName("성공 - MASTER는 배송 상태 변경 가능")
        void success_master() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());
            DeliveryStatusUpdateRequestDto request = new DeliveryStatusUpdateRequestDto(DeliveryStatus.HUB_MOVING);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            DeliveryResponseDto result = deliveryService.updateDeliveryStatus(keycloakSub, deliveryId, request);

            // then
            assertThat(result.deliveryStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);
        }

        @Test
        @DisplayName("성공 - DELIVERY_MANAGER는 본인 담당 배송 상태 변경 가능")
        void success_delivery_manager() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(userId).role(UserRole.DELIVERY_MANAGER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());
            delivery.assignDeliveryPerson(userId);
            DeliveryStatusUpdateRequestDto request = new DeliveryStatusUpdateRequestDto(DeliveryStatus.HUB_MOVING);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            DeliveryResponseDto result = deliveryService.updateDeliveryStatus(keycloakSub, deliveryId, request);

            // then
            assertThat(result.deliveryStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);
        }

        @Test
        @DisplayName("실패 - SUPPLIER_MANAGER는 항상 FORBIDDEN")
        void fail_supplier_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.SUPPLIER_MANAGER).companyId(companyId).build();
            Delivery delivery = createDelivery(companyId);
            DeliveryStatusUpdateRequestDto request = new DeliveryStatusUpdateRequestDto(DeliveryStatus.HUB_MOVING);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.updateDeliveryStatus(keycloakSub, deliveryId, request))
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 배송 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();
            DeliveryStatusUpdateRequestDto request = new DeliveryStatusUpdateRequestDto(DeliveryStatus.HUB_MOVING);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.updateDeliveryStatus(keycloakSub, deliveryId, request))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("updateDeliveryRoute()")
    class UpdateDeliveryRoute {

        private final String keycloakSub = "sub-" + UUID.randomUUID();

        @Test
        @DisplayName("성공 - MASTER는 배송 경로 수정 가능, DeliveryRouteService에 위임")
        void success_master() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());
            DeliveryRouteUpdateRequestDto request = new DeliveryRouteUpdateRequestDto(new BigDecimal("150.0"), 90);
            DeliveryRouteResponseDto mockResponse = new DeliveryRouteResponseDto(
                    routeId, deliveryId, UUID.randomUUID(), UUID.randomUUID(),
                    null, DeliveryRouteStatus.MOVING, 1,
                    new BigDecimal("100.5"), 60, new BigDecimal("150.0"), 90, LocalDateTime.now()
            );

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
            given(deliveryRouteService.updateRoute(routeId, deliveryId, request)).willReturn(mockResponse);

            // when
            DeliveryRouteResponseDto result = deliveryService.updateDeliveryRoute(keycloakSub, deliveryId, routeId, request);

            // then
            assertThat(result.actualDistance()).isEqualByComparingTo(new BigDecimal("150.0"));
            assertThat(result.actualDuration()).isEqualTo(90);
            verify(deliveryRouteService).updateRoute(routeId, deliveryId, request);
        }

        @Test
        @DisplayName("실패 - SUPPLIER_MANAGER는 항상 FORBIDDEN, 경로 서비스 호출 안 함")
        void fail_supplier_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.SUPPLIER_MANAGER).companyId(companyId).build();
            Delivery delivery = createDelivery(companyId);
            DeliveryRouteUpdateRequestDto request = new DeliveryRouteUpdateRequestDto(new BigDecimal("150.0"), 90);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.updateDeliveryRoute(keycloakSub, deliveryId, routeId, request))
                    .isInstanceOf(BaseException.class);
            verify(deliveryRouteService, never()).updateRoute(any(), any(), any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 배송 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();
            DeliveryRouteUpdateRequestDto request = new DeliveryRouteUpdateRequestDto(new BigDecimal("150.0"), 90);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.updateDeliveryRoute(keycloakSub, deliveryId, routeId, request))
                    .isInstanceOf(BaseException.class);
            verify(deliveryRouteService, never()).updateRoute(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("updateDeliveryRouteStatus()")
    class UpdateDeliveryRouteStatus {

        private final String keycloakSub = "sub-" + UUID.randomUUID();

        @Test
        @DisplayName("성공 - MASTER는 배송 경로 상태 변경 가능, DeliveryRouteService에 위임")
        void success_master() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());
            DeliveryRouteStatusUpdateRequestDto request = new DeliveryRouteStatusUpdateRequestDto(DeliveryRouteStatus.MOVING);
            DeliveryRouteResponseDto mockResponse = new DeliveryRouteResponseDto(
                    routeId, deliveryId, UUID.randomUUID(), UUID.randomUUID(),
                    null, DeliveryRouteStatus.MOVING, 1,
                    new BigDecimal("100.5"), 60, null, null, LocalDateTime.now()
            );

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
            given(deliveryRouteService.updateRouteStatus(routeId, deliveryId, request)).willReturn(mockResponse);

            // when
            DeliveryRouteResponseDto result = deliveryService.updateDeliveryRouteStatus(keycloakSub, deliveryId, routeId, request);

            // then
            assertThat(result.status()).isEqualTo(DeliveryRouteStatus.MOVING);
            verify(deliveryRouteService).updateRouteStatus(routeId, deliveryId, request);
        }

        @Test
        @DisplayName("실패 - SUPPLIER_MANAGER는 항상 FORBIDDEN, 경로 서비스 호출 안 함")
        void fail_supplier_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.SUPPLIER_MANAGER).companyId(companyId).build();
            Delivery delivery = createDelivery(companyId);
            DeliveryRouteStatusUpdateRequestDto request = new DeliveryRouteStatusUpdateRequestDto(DeliveryRouteStatus.MOVING);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.updateDeliveryRouteStatus(keycloakSub, deliveryId, routeId, request))
                    .isInstanceOf(BaseException.class);
            verify(deliveryRouteService, never()).updateRouteStatus(any(), any(), any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 배송 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();
            DeliveryRouteStatusUpdateRequestDto request = new DeliveryRouteStatusUpdateRequestDto(DeliveryRouteStatus.MOVING);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.updateDeliveryRouteStatus(keycloakSub, deliveryId, routeId, request))
                    .isInstanceOf(BaseException.class);
            verify(deliveryRouteService, never()).updateRouteStatus(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("deleteDelivery()")
    class DeleteDelivery {

        private final String keycloakSub = "sub-" + UUID.randomUUID();

        @Test
        @DisplayName("성공 - MASTER는 모든 배송 삭제 가능")
        void success_master() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(userId).role(UserRole.MASTER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            deliveryService.deleteDelivery(keycloakSub, deliveryId);

            // then
            verify(deliveryRouteService).deleteAllByDelivery(deliveryId, userId);
        }

        @Test
        @DisplayName("성공 - HUB_MANAGER는 소속 허브의 배송 삭제 가능")
        void success_hub_manager() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(userId).role(UserRole.HUB_MANAGER).hubId(hubId).build();
            Delivery delivery = Delivery.create(
                    UUID.randomUUID(), UUID.randomUUID(), hubId, UUID.randomUUID(),
                    new AddressVO("12345", "서울시 송파구 송파대로 55", "101호"),
                    "홍길동", "U12345"
            );

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            deliveryService.deleteDelivery(keycloakSub, deliveryId);

            // then
            verify(deliveryRouteService).deleteAllByDelivery(deliveryId, userId);
        }

        @Test
        @DisplayName("실패 - HUB_MANAGER가 다른 허브 배송 삭제 시 FORBIDDEN")
        void fail_hub_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.HUB_MANAGER).hubId(UUID.randomUUID()).build();
            Delivery delivery = createDelivery(UUID.randomUUID());

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.deleteDelivery(keycloakSub, deliveryId))
                    .isInstanceOf(BaseException.class);
            verify(deliveryRouteService, never()).deleteAllByDelivery(any(), any());
        }

        @Test
        @DisplayName("실패 - DELIVERY_MANAGER는 항상 FORBIDDEN")
        void fail_delivery_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(userId).role(UserRole.DELIVERY_MANAGER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());
            delivery.assignDeliveryPerson(userId);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.deleteDelivery(keycloakSub, deliveryId))
                    .isInstanceOf(BaseException.class);
            verify(deliveryRouteService, never()).deleteAllByDelivery(any(), any());
        }

        @Test
        @DisplayName("실패 - SUPPLIER_MANAGER는 항상 FORBIDDEN")
        void fail_supplier_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.SUPPLIER_MANAGER).companyId(companyId).build();
            Delivery delivery = createDelivery(companyId);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.deleteDelivery(keycloakSub, deliveryId))
                    .isInstanceOf(BaseException.class);
            verify(deliveryRouteService, never()).deleteAllByDelivery(any(), any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 배송 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.deleteDelivery(keycloakSub, deliveryId))
                    .isInstanceOf(BaseException.class);
            verify(deliveryRouteService, never()).deleteAllByDelivery(any(), any());
        }
    }

    @Nested
    @DisplayName("deleteDeliveryRoute()")
    class DeleteDeliveryRoute {

        private final String keycloakSub = "sub-" + UUID.randomUUID();

        @Test
        @DisplayName("성공 - MASTER는 배송 경로 삭제 가능, DeliveryRouteService에 위임")
        void success_master() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(userId).role(UserRole.MASTER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when
            deliveryService.deleteDeliveryRoute(keycloakSub, deliveryId, routeId);

            // then
            verify(deliveryRouteService).deleteRoute(routeId, deliveryId, userId);
        }

        @Test
        @DisplayName("실패 - DELIVERY_MANAGER는 항상 FORBIDDEN, 경로 서비스 호출 안 함")
        void fail_delivery_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(userId).role(UserRole.DELIVERY_MANAGER).build();
            Delivery delivery = createDelivery(UUID.randomUUID());
            delivery.assignDeliveryPerson(userId);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.deleteDeliveryRoute(keycloakSub, deliveryId, routeId))
                    .isInstanceOf(BaseException.class);
            verify(deliveryRouteService, never()).deleteRoute(any(), any(), any());
        }

        @Test
        @DisplayName("실패 - SUPPLIER_MANAGER는 항상 FORBIDDEN, 경로 서비스 호출 안 함")
        void fail_supplier_manager_forbidden() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.SUPPLIER_MANAGER).companyId(companyId).build();
            Delivery delivery = createDelivery(companyId);

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

            // when & then
            assertThatThrownBy(() -> deliveryService.deleteDeliveryRoute(keycloakSub, deliveryId, routeId))
                    .isInstanceOf(BaseException.class);
            verify(deliveryRouteService, never()).deleteRoute(any(), any(), any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 배송 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UserResponseDto userInfo = UserResponseDto.builder().id(UUID.randomUUID()).role(UserRole.MASTER).build();

            given(userServiceClient.getUserBySub(keycloakSub)).willReturn(ApiResponse.success(userInfo));
            given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.deleteDeliveryRoute(keycloakSub, deliveryId, routeId))
                    .isInstanceOf(BaseException.class);
            verify(deliveryRouteService, never()).deleteRoute(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getActiveDeliveryCount()")
    class GetActiveDeliveryCount {

        @Test
        @DisplayName("성공 - 진행 중 배송이 있을 때 건수 반환")
        void success_returns_count() {
            // given
            UUID hubId = UUID.randomUUID();
            given(deliveryRepository.countActiveByHubId(hubId, List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELED)))
                    .willReturn(3);

            // when
            int result = deliveryService.getActiveDeliveryCount(hubId);

            // then
            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("성공 - 진행 중 배송이 없을 때 0 반환")
        void success_returns_zero() {
            // given
            UUID hubId = UUID.randomUUID();
            given(deliveryRepository.countActiveByHubId(hubId, List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELED)))
                    .willReturn(0);

            // when
            int result = deliveryService.getActiveDeliveryCount(hubId);

            // then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("성공 - DELIVERED, CANCELED 상태를 제외 목록으로 전달")
        void success_passes_correct_excluded_statuses() {
            // given
            UUID hubId = UUID.randomUUID();
            given(deliveryRepository.countActiveByHubId(any(), any())).willReturn(0);

            // when
            deliveryService.getActiveDeliveryCount(hubId);

            // then
            verify(deliveryRepository).countActiveByHubId(hubId,
                    List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELED));
        }
    }
}