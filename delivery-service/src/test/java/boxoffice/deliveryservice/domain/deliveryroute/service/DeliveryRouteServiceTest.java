package boxoffice.deliveryservice.domain.deliveryroute.service;

import com.boxoffice.common.entity.AddressVO;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import boxoffice.deliveryservice.domain.deliveryroute.dto.request.DeliveryRouteStatusUpdateRequestDto;
import boxoffice.deliveryservice.domain.deliveryroute.dto.request.DeliveryRouteUpdateRequestDto;
import boxoffice.deliveryservice.domain.deliveryroute.dto.response.DeliveryRouteResponseDto;
import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRoute;
import boxoffice.deliveryservice.domain.deliveryroute.entity.DeliveryRouteStatus;
import boxoffice.deliveryservice.domain.deliveryroute.repository.DeliveryRouteRepository;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.PageResponse;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DisplayName("DeliveryRouteService 테스트")
@ExtendWith(MockitoExtension.class)
class DeliveryRouteServiceTest {

    @InjectMocks
    private DeliveryRouteService deliveryRouteService;

    @Mock
    private DeliveryRouteRepository deliveryRouteRepository;

    private Delivery createDelivery() {
        return Delivery.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new AddressVO("12345", "서울시 송파구 송파대로 55", "101호"),
                "홍길동", "U12345"
        );
    }

    private DeliveryRoute createRoute(Delivery delivery, int sequence) {
        return DeliveryRoute.builder()
                .delivery(delivery)
                .originHubId(UUID.randomUUID())
                .destinationHubId(UUID.randomUUID())
                .sequence(sequence)
                .expectedDistance(new BigDecimal("100.5"))
                .expectedDuration(60)
                .status(DeliveryRouteStatus.WAITING)
                .build();
    }

    @Nested
    @DisplayName("getRoutesByDelivery()")
    class GetRoutesByDelivery {

        @Test
        @DisplayName("성공 - 배송 경로 목록 반환")
        void success() {
            // given
            UUID deliveryId = UUID.randomUUID();
            PageRequest pageable = PageRequest.of(0, 10, Sort.by("sequence").ascending());
            Delivery delivery = createDelivery();
            DeliveryRoute route1 = createRoute(delivery, 1);
            DeliveryRoute route2 = createRoute(delivery, 2);
            Page<DeliveryRoute> page = new PageImpl<>(List.of(route1, route2), pageable, 2);
            given(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNull(deliveryId, pageable)).willReturn(page);

            // when
            PageResponse<DeliveryRouteResponseDto> result = deliveryRouteService.getRoutesByDelivery(deliveryId, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent().get(0).sequence()).isEqualTo(1);
            assertThat(result.getContent().get(1).sequence()).isEqualTo(2);
            assertThat(result.getContent().get(0).status()).isEqualTo(DeliveryRouteStatus.WAITING);
        }

        @Test
        @DisplayName("성공 - 빈 목록 반환")
        void success_empty() {
            // given
            UUID deliveryId = UUID.randomUUID();
            PageRequest pageable = PageRequest.of(0, 10);
            Page<DeliveryRoute> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            given(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNull(deliveryId, pageable)).willReturn(emptyPage);

            // when
            PageResponse<DeliveryRouteResponseDto> result = deliveryRouteService.getRoutesByDelivery(deliveryId, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("getRouteByDelivery()")
    class GetRouteByDelivery {

        @Test
        @DisplayName("성공 - 배송 경로 단건 반환")
        void success() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            Delivery delivery = createDelivery();
            DeliveryRoute route = createRoute(delivery, 1);
            given(deliveryRouteRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(routeId, deliveryId))
                    .willReturn(Optional.of(route));

            // when
            DeliveryRouteResponseDto result = deliveryRouteService.getRouteByDelivery(deliveryId, routeId);

            // then
            assertThat(result.sequence()).isEqualTo(1);
            assertThat(result.status()).isEqualTo(DeliveryRouteStatus.WAITING);
            assertThat(result.expectedDistance()).isEqualByComparingTo(new BigDecimal("100.5"));
            assertThat(result.expectedDuration()).isEqualTo(60);
            assertThat(result.actualDistance()).isNull();
            assertThat(result.actualDuration()).isNull();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 경로 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            given(deliveryRouteRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(routeId, deliveryId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryRouteService.getRouteByDelivery(deliveryId, routeId))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("updateRoute()")
    class UpdateRoute {

        @Test
        @DisplayName("성공 - 실제 거리/소요시간 업데이트")
        void success() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            Delivery delivery = createDelivery();
            DeliveryRoute route = createRoute(delivery, 1);
            DeliveryRouteUpdateRequestDto request = new DeliveryRouteUpdateRequestDto(new BigDecimal("150.5"), 90);

            given(deliveryRouteRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(routeId, deliveryId))
                    .willReturn(Optional.of(route));

            // when
            DeliveryRouteResponseDto result = deliveryRouteService.updateRoute(routeId, deliveryId, request);

            // then
            assertThat(result.actualDistance()).isEqualByComparingTo(new BigDecimal("150.5"));
            assertThat(result.actualDuration()).isEqualTo(90);
            assertThat(result.expectedDistance()).isEqualByComparingTo(new BigDecimal("100.5"));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 경로 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            DeliveryRouteUpdateRequestDto request = new DeliveryRouteUpdateRequestDto(new BigDecimal("150.5"), 90);

            given(deliveryRouteRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(routeId, deliveryId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryRouteService.updateRoute(routeId, deliveryId, request))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("updateRouteStatus()")
    class UpdateRouteStatus {

        @Test
        @DisplayName("성공 - 경로 상태 변경")
        void success() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            Delivery delivery = createDelivery();
            DeliveryRoute route = createRoute(delivery, 1);
            DeliveryRouteStatusUpdateRequestDto request = new DeliveryRouteStatusUpdateRequestDto(DeliveryRouteStatus.MOVING);

            given(deliveryRouteRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(routeId, deliveryId))
                    .willReturn(Optional.of(route));

            // when
            DeliveryRouteResponseDto result = deliveryRouteService.updateRouteStatus(routeId, deliveryId, request);

            // then
            assertThat(result.status()).isEqualTo(DeliveryRouteStatus.MOVING);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 경로 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            DeliveryRouteStatusUpdateRequestDto request = new DeliveryRouteStatusUpdateRequestDto(DeliveryRouteStatus.MOVING);

            given(deliveryRouteRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(routeId, deliveryId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryRouteService.updateRouteStatus(routeId, deliveryId, request))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("deleteRoute()")
    class DeleteRoute {

        @Test
        @DisplayName("성공 - 경로 soft-delete")
        void success() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();
            UUID deletedBy = UUID.randomUUID();
            Delivery delivery = createDelivery();
            DeliveryRoute route = createRoute(delivery, 1);

            given(deliveryRouteRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(routeId, deliveryId))
                    .willReturn(Optional.of(route));

            // when
            deliveryRouteService.deleteRoute(routeId, deliveryId, deletedBy);

            // then
            assertThat(route.isDeleted()).isTrue();
            assertThat(route.getDeletedBy()).isEqualTo(deletedBy);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 경로 ID")
        void fail_not_found() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID routeId = UUID.randomUUID();

            given(deliveryRouteRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(routeId, deliveryId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryRouteService.deleteRoute(routeId, deliveryId, UUID.randomUUID()))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("deleteAllByDelivery()")
    class DeleteAllByDelivery {

        @Test
        @DisplayName("성공 - 배송에 속한 모든 경로 soft-delete")
        void success() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID deletedBy = UUID.randomUUID();
            Delivery delivery = createDelivery();
            DeliveryRoute route1 = createRoute(delivery, 1);
            DeliveryRoute route2 = createRoute(delivery, 2);

            given(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNull(deliveryId))
                    .willReturn(List.of(route1, route2));

            // when
            deliveryRouteService.deleteAllByDelivery(deliveryId, deletedBy);

            // then
            assertThat(route1.isDeleted()).isTrue();
            assertThat(route2.isDeleted()).isTrue();
            assertThat(route1.getDeletedBy()).isEqualTo(deletedBy);
            assertThat(route2.getDeletedBy()).isEqualTo(deletedBy);
        }

        @Test
        @DisplayName("성공 - 경로가 없어도 예외 없이 종료")
        void success_empty_routes() {
            // given
            UUID deliveryId = UUID.randomUUID();
            UUID deletedBy = UUID.randomUUID();

            given(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNull(deliveryId))
                    .willReturn(List.of());

            // when & then (no exception)
            deliveryRouteService.deleteAllByDelivery(deliveryId, deletedBy);
        }
    }
}
