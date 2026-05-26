package boxoffice.orderservice.application.client.dto;

import java.util.UUID;

public record UserDetailInfo(
    UUID userId,
    String email,
    String username,
    String role,
    UUID hubId,
    String status,
    UUID companyId
) {
  public boolean isMaster() {
    return "MASTER".equals(this.role);
  }

  public boolean isHubManager() {
    return "HUB_MANAGER".equals(this.role);
  }

  public boolean isDeliveryManager() {
    return "DELIVERY_MANAGER".equals(this.role);
  }

  public boolean isSupplierManager() {
    return "SUPPLIER_MANAGER".equals(this.role);
  }
}
