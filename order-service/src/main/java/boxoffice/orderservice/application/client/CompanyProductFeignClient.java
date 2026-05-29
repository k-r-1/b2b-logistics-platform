package boxoffice.orderservice.application.client;

import boxoffice.orderservice.application.client.dto.request.StockCheckRequest;
import boxoffice.orderservice.application.client.dto.request.StockDeductRequest;
import boxoffice.orderservice.application.client.dto.request.StockRestoreRequest;
import boxoffice.orderservice.application.client.dto.response.InternalCompanyHub;
import boxoffice.orderservice.application.client.dto.response.StockCheckResponse;
import boxoffice.orderservice.application.client.dto.response.StockDeductResponse;
import boxoffice.orderservice.application.client.fallback.CompanyProductFeignClientFallbackFactory;
import com.boxoffice.common.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "company-service", contextId = "companyProductFeignClient", fallbackFactory = CompanyProductFeignClientFallbackFactory.class, primary = false)
public interface CompanyProductFeignClient {

  @GetMapping("/internal/v1/companies/hubs/{supplierId}/{receiverId}")
  ApiResponse<InternalCompanyHub> getCompanyById(@PathVariable UUID supplierId, @PathVariable UUID receiverId);

  @PostMapping("/internal/v1/products/stocks/check")
  ApiResponse<StockCheckResponse> checkStocks(@RequestBody List<StockCheckRequest> requests);

  // orderId를 선제적으로 전달하여 재고 차감 이력 추적 가능
  @PostMapping("/internal/v1/products/stocks/deduct")
  ApiResponse<StockDeductResponse> deductStocks(@RequestParam UUID orderId, @RequestBody StockDeductRequest requests);

  @PostMapping("/internal/v1/products/stocks/restore")
  void restoreStocks(@RequestParam UUID orderId, @RequestBody List<StockRestoreRequest> requests);
}
