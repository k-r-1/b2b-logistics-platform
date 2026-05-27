package boxoffice.orderservice.application.client.fallback;

import boxoffice.orderservice.application.client.DeliveryFeignClient;
import boxoffice.orderservice.application.client.dto.request.DeliveryCancelRequest;
import boxoffice.orderservice.application.client.dto.request.DeliveryCreateRequest;
import boxoffice.orderservice.application.client.dto.response.DeliveryResponseDto;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeliveryFeignClientFallbackFactory implements FallbackFactory<DeliveryFeignClient> {

    @Override
    public DeliveryFeignClient create(Throwable cause) {
        return new DeliveryFeignClient() {
            @Override
            public DeliveryResponseDto requestDelivery(DeliveryCreateRequest request) {
                log.error("[DeliveryFeignClient] 배송 요청 실패. orderId={}", request.orderId(), cause);
                throw new BaseException(OrderErrorCode.DELIVERY_REQUEST_FAILED);
            }

            @Override
            public void cancelDelivery(DeliveryCancelRequest request) {
                // 배송 취소 실패 - 예외를 삼키고 로그만 기록 (수동 처리 대기)
                log.error("[DeliveryFeignClient] 배송 취소 실패 - 수동 처리 필요. orderId={}", request.orderId(), cause);
            }
        };
    }
}
