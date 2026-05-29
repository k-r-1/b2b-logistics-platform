package boxoffice.orderservice.application.client.fallback;

import boxoffice.orderservice.application.client.CompanyProductFeignClient;
import boxoffice.orderservice.application.client.dto.request.StockCheckRequest;
import boxoffice.orderservice.application.client.dto.request.StockDeductRequest;
import boxoffice.orderservice.application.client.dto.request.StockRestoreRequest;
import boxoffice.orderservice.application.client.dto.response.InternalCompanyHub;
import boxoffice.orderservice.application.client.dto.response.StockCheckResponse;
import boxoffice.orderservice.application.client.dto.response.StockDeductResponse;
import boxoffice.orderservice.infra.exception.OrderErrorCode;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.ApiResponse;
import java.util.List;  // checkStocks, restoreStocks용
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CompanyProductFeignClientFallbackFactory implements FallbackFactory<CompanyProductFeignClient> {

    @Override
    public CompanyProductFeignClient create(Throwable cause) {
        return new CompanyProductFeignClient() {
            @Override
            public ApiResponse<InternalCompanyHub> getCompanyById(UUID supplierId, UUID receiverId) {
                log.error("[CompanyProductFeignClient] 허브 정보 조회 실패. supplierId={}, receiverId={}",
                    supplierId, receiverId, cause);
                throw new BaseException(OrderErrorCode.STOCK_DEDUCT_FAILED);
            }

            @Override
            public ApiResponse<StockCheckResponse> checkStocks(List<StockCheckRequest> requests) {
                log.error("[CompanyProductFeignClient] 재고 확인 실패", cause);
                throw new BaseException(OrderErrorCode.STOCK_DEDUCT_FAILED);
            }

            @Override
            public ApiResponse<StockDeductResponse> deductStocks(UUID orderId, StockDeductRequest requests) {
                log.error("[CompanyProductFeignClient] 재고 차감 실패. orderId={}", orderId, cause);
                throw new BaseException(OrderErrorCode.STOCK_DEDUCT_FAILED);
            }

            @Override
            public void restoreStocks(UUID orderId, List<StockRestoreRequest> requests) {
                // 보상 트랜잭션 실패 - 로그만 기록
                log.error("[CompanyProductFeignClient] 재고 복구 실패 - 수동 처리 필요", cause);
            }
        };
    }
}
