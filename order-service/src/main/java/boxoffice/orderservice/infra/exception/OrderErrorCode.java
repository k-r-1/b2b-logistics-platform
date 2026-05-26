package boxoffice.orderservice.infra.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {
  INVALID_PRICE(HttpStatus.BAD_REQUEST, "ORDER_001", "")
  ;

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

}
