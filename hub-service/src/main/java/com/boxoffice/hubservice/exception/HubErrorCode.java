package com.boxoffice.hubservice.exception;

import com.boxoffice.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum HubErrorCode implements ErrorCode {

    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB-001", "허브를 찾을 수 없습니다."),
    HUB_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB-002", "허브 경로를 찾을 수 없습니다."),
    TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB-003", "재고 이전을 찾을 수 없습니다."),
    INVALID_HUB_TYPE(HttpStatus.BAD_REQUEST, "HUB-004", "CENTRAL 또는 REGIONAL만 생성 가능합니다."),
    HUB_INACTIVE(HttpStatus.BAD_REQUEST, "HUB-005", "INACTIVE 상태의 허브에서는 허용되지 않는 작업입니다."),
    HUB_NOT_INACTIVE(HttpStatus.BAD_REQUEST, "HUB-006", "삭제 전 먼저 deactivate를 호출하세요."),
    HUB_ALREADY_INACTIVE(HttpStatus.BAD_REQUEST, "HUB-007", "이미 INACTIVE 상태인 허브입니다."),
    DUPLICATE_HUB_NAME(HttpStatus.CONFLICT, "HUB-008", "이미 존재하는 허브 이름입니다."),
    DUPLICATE_HUB_ROUTE(HttpStatus.CONFLICT, "HUB-009", "이미 존재하는 경로입니다."),
    SAME_HUB_ROUTE(HttpStatus.BAD_REQUEST, "HUB-010", "출발 허브와 도착 허브가 동일한 경로는 생성할 수 없습니다."),
    SAME_HUB_PATH(HttpStatus.BAD_REQUEST,  "HUB-011", "출발 허브와 도착 허브가 동일하면 경로를 계산할 수 없습니다."),
    HUB_INACTIVE_IN_PATH(HttpStatus.BAD_REQUEST, "HUB-012", "경로에 INACTIVE 허브가 포함되어 있습니다."),
    INVALID_RATIO_SUM(HttpStatus.BAD_REQUEST, "HUB-013", "이전 비율의 합계가 1.0이 아닙니다."),
    INVALID_TARGET_HUB(HttpStatus.BAD_REQUEST, "HUB-014", "이전 대상 허브가 INACTIVE 또는 삭제 상태입니다."),
    TRANSFER_INVALID_STATUS(HttpStatus.BAD_REQUEST, "HUB-015", "현재 상태에서 허용되지 않는 작업입니다."),
    TRANSFER_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "HUB-016", "이미 완료된 재고 이전입니다."),
    HUB_HAS_ACTIVE_DELIVERY(HttpStatus.CONFLICT, "HUB-017", "진행 중인 배송이 있어 삭제할 수 없습니다."),
    HUB_HAS_STOCK(HttpStatus.CONFLICT, "HUB-018", "재고가 남아 있어 삭제할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
