package boxoffice.orderservice.domain.vo;

import java.util.UUID;

public record OrderSearchCondition(UUID companyId, UUID hubId) {

    public static OrderSearchCondition forCompany(UUID companyId) {
        return new OrderSearchCondition(companyId, null);
    }

    public static OrderSearchCondition forHub(UUID hubId) {
        return new OrderSearchCondition(null, hubId);
    }

    public static OrderSearchCondition forMaster() {
        return new OrderSearchCondition(null, null);
    }
}
