package boxoffice.orderservice.application.client;

import boxoffice.orderservice.application.client.dto.request.StockCheckRequest;
import boxoffice.orderservice.application.client.dto.request.StockDeductRequest;
import boxoffice.orderservice.application.client.dto.request.StockRestoreRequest;
import boxoffice.orderservice.application.client.dto.response.StockCheckResponse;
import jakarta.ws.rs.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "company-product-service")
public interface CompanyProductFeignClient {
  // 주문 전 상품 존재 여부, 삭제 여부, 재고 상태 검증
  @GetMapping("/internal/products/check")
  StockCheckResponse checkStocks(@RequestBody List<StockCheckRequest> requests);

  // 재고 차감 요청
  @PostMapping("/internal/products/deduct")
  void deductStocks(@RequestBody List<StockDeductRequest> requests);

    // 복수 상품 재고 일괄 복원 : 보상 트랜잭션
  @PostMapping("/internal/products/restore")
  void restoreStocks(@RequestBody List<StockRestoreRequest> requests);

}
