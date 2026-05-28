package boxoffice.deliveryservice.domain.delivery.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DeliveryErrorCode implements ErrorCode {
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY-001", "배송을 찾을 수 없습니다."),
    DELIVERY_ALREADY_CANCELED(HttpStatus.CONFLICT, "DELIVERY-002", "이미 취소된 배송입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
