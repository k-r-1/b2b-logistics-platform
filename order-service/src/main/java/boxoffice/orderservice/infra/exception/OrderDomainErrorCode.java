package boxoffice.orderservice.infra.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderDomainErrorCode implements ErrorCode {
  INVALID_COMPANY_ID(HttpStatus.BAD_REQUEST, "ORDER_DOMAIN_001", "업체 ID가 올바르지 않습니다."),
  INVALID_ORDER_PRODUCT(HttpStatus.BAD_REQUEST, "ORDER_DOMAIN_002", "가격/수량이 형식에 맞지 않습니다."),
  INVALID_PRODUCT_ID(HttpStatus.BAD_REQUEST, "ORDER_DOMAIN_003", "상품 ID가 올바르지 않습니다."),
  EMPTY_ORDER_PRODUCT(HttpStatus.BAD_REQUEST, "ORDER_DOMAIN_004", "주문 상품내역이 없습니다."),
  INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "ORDER_DOMAIN_005", "상태 변경은 순차적으로만 가능합니다."),
  INVALID_DELIVERY_ID(HttpStatus.BAD_REQUEST, "ORDER_DOMAIN_006", "배송 ID가 잘못되었습니다."),
  MISSING_COMPANY_ID(HttpStatus.BAD_REQUEST, "ORDER_DOMAIN_007", "업체 ID가 없습니다."),
  MISSING_HUB_ID(HttpStatus.BAD_REQUEST, "ORDER_DOMAIN_008", "허브 ID가 없습니다."),
  INVALID_PRICE(HttpStatus.BAD_REQUEST, "ORDER_DOMAIN_009", "불가한 금액입니다.")
  ;

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}