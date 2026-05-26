package boxoffice.orderservice.infra.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {
  INVALID_PRICE(HttpStatus.BAD_REQUEST, "ORDER_001", ""),
  NO_PERMISSION(HttpStatus.BAD_REQUEST, "ORDER_002", "본인 회사의 주문은 불가합니다."),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "ORDER_003", "주문 불가능"),
  INVALID_STATUS(HttpStatus.BAD_REQUEST, "ORDER_004", "배송중이면 상태를 변경할 수 없습니다."),
  ORDER_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_005", "주문 생성 중에 오류가 발생했습니다.")
  ;

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

}
