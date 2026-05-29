package boxoffice.orderservice.infra.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {
  // 권한
  UNAUTHORIZED_ORDER(HttpStatus.FORBIDDEN, "ORDER_001", "해당 주문을 생성할 권한이 없습니다."),
  UNAUTHORIZED_HUB_ORDER(HttpStatus.FORBIDDEN, "ORDER_002", "본인 관찰 허브와 연관된 주문만 생성할 수 있습니다."),

  // 주문 상태
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_011", "존재하지 않는 주문입니다."),
  ORDER_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_012", "주문 저장에 실패했습니다."),
  CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ORDER_013", "PENDING 상태의 주문만 취소할 수 있습니다."),
  ORDER_ALREADY_DELIVERED(HttpStatus.BAD_REQUEST, "ORDER_014", "배송 완료된 주문은 수정할 수 없습니다."),

  // 상품
  EMPTY_ORDER_PRODUCT(HttpStatus.BAD_REQUEST, "ORDER_PRODUCT_001", "주문 상품은 최소 1개 이상이어야 합니다."),
  INVALID_ORDER_PRODUCT(HttpStatus.BAD_REQUEST, "ORDER_PRODUCT_002", "상품 가격 또는 수량이 올바르지 않습니다."),
  INVALID_COMPANY_ID(HttpStatus.BAD_REQUEST, "ORDER_PRODUCT_003", "업체 ID가 올바르지 않습니다."),

  // 외부 서비스
  STOCK_DEDUCT_FAILED(HttpStatus.BAD_GATEWAY, "ORDER_021", "재고 차감에 실패했습니다."),
  DELIVERY_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_022", "배송 요청에 실패했습니다."),
  USER_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "ORDER_023", "유저 서비스를 사용할 수 없습니다."),
  ;

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

}
