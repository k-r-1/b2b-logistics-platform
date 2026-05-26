package boxoffice.deliveryservice.domain.deliveryroute.service;

import boxoffice.deliveryservice.domain.delivery.dto.request.DeliveryCreateRequestDto.AddressRequest;
import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
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
                new AddressRequest("12345", "서울시 송파구 송파대로 55", "101호").toAddressVO(),
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
}
