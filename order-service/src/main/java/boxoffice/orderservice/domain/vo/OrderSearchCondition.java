package boxoffice.orderservice.domain.vo;

import boxoffice.orderservice.domain.enums.OrderStatus;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 주문 검색 조건 VO.
 *
 * companyId : SUPPLIER_MANAGER 접근 제어 (supplierId OR receiverId)
 * hubId     : HUB_MANAGER 접근 제어 (sourceHubId OR destinationHubId, 방향 필터 없을 때)
 * filterSourceHubId      : 출발 허브 필터. HUB_MANAGER=자신의 hubId 고정, MASTER=임의 UUID
 * filterDestinationHubId : 도착 허브 필터. 동일 규칙.
 * hubId와 filterSource/Destination은 상호 배타적으로 사용된다.
 */
public record OrderSearchCondition(
    UUID companyId,
    UUID hubId,
    UUID filterSourceHubId,
    UUID filterDestinationHubId,
    OrderStatus status,
    LocalDate startDate,
    LocalDate endDate
) {

    public static OrderSearchCondition forCompany(UUID companyId) {
        return new OrderSearchCondition(companyId, null, null, null, null, null, null);
    }

    public static OrderSearchCondition forCompany(UUID companyId, OrderStatus status, LocalDate startDate, LocalDate endDate) {
        return new OrderSearchCondition(companyId, null, null, null, status, startDate, endDate);
    }

    public static OrderSearchCondition forHub(UUID hubId) {
        return new OrderSearchCondition(null, hubId, null, null, null, null, null);
    }

    /**
     * HUB_MANAGER 조건 빌더.
     * wantSource / wantDest 가 하나라도 true 면 방향 필터로 전환하고 broad OR(hubId)는 해제.
     * 실제 필터 UUID는 항상 userHubId 로 고정된다 (접근 제어).
     */
    public static OrderSearchCondition forHub(
        UUID userHubId,
        boolean wantSource,
        boolean wantDest,
        OrderStatus status,
        LocalDate startDate,
        LocalDate endDate
    ) {
        boolean hasDirectionFilter = wantSource || wantDest;
        return new OrderSearchCondition(
            null,
            hasDirectionFilter ? null : userHubId,
            wantSource ? userHubId : null,
            wantDest ? userHubId : null,
            status,
            startDate,
            endDate
        );
    }

    public static OrderSearchCondition forMaster() {
        return new OrderSearchCondition(null, null, null, null, null, null, null);
    }

    public static OrderSearchCondition forMaster(
        UUID filterSourceHubId,
        UUID filterDestinationHubId,
        OrderStatus status,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new OrderSearchCondition(null, null, filterSourceHubId, filterDestinationHubId, status, startDate, endDate);
    }

    public String cacheKey() {
        return String.join("_",
            companyId != null ? companyId.toString() : "N",
            hubId != null ? hubId.toString() : "N",
            filterSourceHubId != null ? filterSourceHubId.toString() : "N",
            filterDestinationHubId != null ? filterDestinationHubId.toString() : "N",
            status != null ? status.name() : "N",
            startDate != null ? startDate.toString() : "N",
            endDate != null ? endDate.toString() : "N"
        );
    }
}
