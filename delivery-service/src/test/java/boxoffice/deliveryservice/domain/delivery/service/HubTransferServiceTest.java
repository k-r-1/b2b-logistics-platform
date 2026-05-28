package boxoffice.deliveryservice.domain.delivery.service;

import boxoffice.deliveryservice.client.DeliveryManagerClient;
import boxoffice.deliveryservice.client.dto.request.DeliveryManagerAssignRequestDto;
import boxoffice.deliveryservice.client.dto.request.DeliveryManagerAssignRequestDto.DeliveryType;
import boxoffice.deliveryservice.client.dto.response.DeliveryManagerAssignResponseDto;
import boxoffice.deliveryservice.kafka.event.HubTransferDispatchedEvent;
import boxoffice.deliveryservice.kafka.event.TransferAssignFailedEvent;
import boxoffice.deliveryservice.kafka.event.TransferAssignSuccessEvent;
import boxoffice.deliveryservice.kafka.producer.HubTransferResultProducer;
import com.boxoffice.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("HubTransferService 테스트")
@ExtendWith(MockitoExtension.class)
class HubTransferServiceTest {

    @InjectMocks
    private HubTransferService hubTransferService;

    @Mock
    private DeliveryManagerClient deliveryManagerClient;

    @Mock
    private HubTransferResultProducer producer;

    @Nested
    @DisplayName("handleHubTransferDispatched()")
    class HandleHubTransferDispatched {

        @Test
        @DisplayName("성공 - 담당자 배정 후 성공 이벤트 발행")
        void success() {
            // given
            UUID transferId = UUID.randomUUID();
            UUID fromHubId = UUID.randomUUID();
            UUID toHubId = UUID.randomUUID();
            UUID deliveryManagerId = UUID.randomUUID();
            HubTransferDispatchedEvent event = new HubTransferDispatchedEvent(transferId, fromHubId, toHubId);

            given(deliveryManagerClient.assignDeliveryManager(any()))
                    .willReturn(ApiResponse.success(new DeliveryManagerAssignResponseDto(deliveryManagerId)));

            // when
            hubTransferService.handleHubTransferDispatched(event);

            // then
            ArgumentCaptor<TransferAssignSuccessEvent> captor = ArgumentCaptor.forClass(TransferAssignSuccessEvent.class);
            verify(producer).publishSuccess(captor.capture());
            assertThat(captor.getValue().transferId()).isEqualTo(transferId);
            assertThat(captor.getValue().deliveryManagerId()).isEqualTo(deliveryManagerId);
            verify(producer, never()).publishFailure(any());
        }

        @Test
        @DisplayName("성공 - fromHubId와 HUB_TO_HUB 타입으로 배정 요청")
        void success_request_with_correct_hub_and_type() {
            // given
            UUID transferId = UUID.randomUUID();
            UUID fromHubId = UUID.randomUUID();
            HubTransferDispatchedEvent event = new HubTransferDispatchedEvent(transferId, fromHubId, UUID.randomUUID());

            given(deliveryManagerClient.assignDeliveryManager(any()))
                    .willReturn(ApiResponse.success(new DeliveryManagerAssignResponseDto(UUID.randomUUID())));

            // when
            hubTransferService.handleHubTransferDispatched(event);

            // then
            ArgumentCaptor<DeliveryManagerAssignRequestDto> captor = ArgumentCaptor.forClass(DeliveryManagerAssignRequestDto.class);
            verify(deliveryManagerClient).assignDeliveryManager(captor.capture());
            assertThat(captor.getValue().hubId()).isEqualTo(fromHubId);
            assertThat(captor.getValue().deliveryType()).isEqualTo(DeliveryType.HUB_TO_HUB);
        }

        @Test
        @DisplayName("실패 - Feign 예외 발생 시 실패 이벤트 발행")
        void fail_when_feign_throws() {
            // given
            UUID transferId = UUID.randomUUID();
            HubTransferDispatchedEvent event = new HubTransferDispatchedEvent(transferId, UUID.randomUUID(), UUID.randomUUID());
            String errorMessage = "delivery-manager-service 호출 실패";

            given(deliveryManagerClient.assignDeliveryManager(any()))
                    .willThrow(new RuntimeException(errorMessage));

            // when
            hubTransferService.handleHubTransferDispatched(event);

            // then
            ArgumentCaptor<TransferAssignFailedEvent> captor = ArgumentCaptor.forClass(TransferAssignFailedEvent.class);
            verify(producer).publishFailure(captor.capture());
            assertThat(captor.getValue().transferId()).isEqualTo(transferId);
            assertThat(captor.getValue().reason()).isEqualTo(errorMessage);
            verify(producer, never()).publishSuccess(any());
        }

        @Test
        @DisplayName("실패 - 예외 발생 시 외부로 전파하지 않음")
        void fail_exception_not_propagated() {
            // given
            HubTransferDispatchedEvent event = new HubTransferDispatchedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            given(deliveryManagerClient.assignDeliveryManager(any()))
                    .willThrow(new RuntimeException("오류"));

            // when & then
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> hubTransferService.handleHubTransferDispatched(event)
            );
        }
    }
}