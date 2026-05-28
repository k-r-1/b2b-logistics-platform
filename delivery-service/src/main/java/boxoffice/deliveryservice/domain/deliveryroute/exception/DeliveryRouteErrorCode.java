package boxoffice.deliveryservice.domain.deliveryroute.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DeliveryRouteErrorCode implements ErrorCode {
    DELIVERY_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY-ROUTE-001", "배송 경로을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
