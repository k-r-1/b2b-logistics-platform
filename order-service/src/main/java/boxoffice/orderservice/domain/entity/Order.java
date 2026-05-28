package boxoffice.orderservice.domain.entity;

import boxoffice.orderservice.domain.enums.OrderStatus;
import boxoffice.orderservice.domain.vo.TotalPrice;
import boxoffice.orderservice.infra.exception.OrderDomainErrorCode;
import com.boxoffice.common.entity.BaseEntity;
import com.boxoffice.common.exception.BaseException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "p_orders",
    indexes = {
        @Index(name = "idx_order_supplier", columnList = "supplier_id"),
        @Index(name = "idx_order_receiver", columnList = "receiver_id"),
        @Index(name = "idx_order_source_hub", columnList = "source_hub_id"),
        @Index(name = "idx_order_destination_hub", columnList = "destination_hub_id")
    }
)
@SQLRestriction("deleted_at IS NULL")
public class Order extends BaseEntity {

  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;

  @Column(name = "receiver_id", nullable = false)
  private UUID receiverId;

  @Column(name = "source_hub_id", nullable = false)
  private UUID sourceHubId;

  @Column(name = "destination_hub_id", nullable = false)
  private UUID destinationHubId;

  @Column(name = "delivery_id")
  private UUID deliveryId;

  @Embedded
  private TotalPrice totalPrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private OrderStatus status;

  @Column(name = "request", length = 100)
  private String request;

  @ElementCollection
  @CollectionTable(
      name = "p_order_products",
      joinColumns = @JoinColumn(name = "order_id")
  )
  private List<OrderProduct> orderProducts = new ArrayList<>();

  public static Order create(
      UUID orderId,
      UUID supplierId,
      UUID receiverId,
      UUID sourceHubId,
      UUID destinationHubId,
      String request,
      List<OrderProduct> orderProducts) {
    validateCompanyId(supplierId);
    validateCompanyId(receiverId);
    validateHubId(sourceHubId);
    validateHubId(destinationHubId);
    validateOrderProducts(orderProducts);

    Order order = new Order();
    order.assignId(orderId);
    order.supplierId = supplierId;
    order.receiverId = receiverId;
    order.sourceHubId = sourceHubId;
    order.destinationHubId = destinationHubId;
    order.request = request;
    order.status = OrderStatus.PENDING;
    order.orderProducts = new ArrayList<>(orderProducts);
    order.totalPrice = TotalPrice.create(order.calculateTotalPrice());
    return order;
  }

  public void linkDelivery(UUID deliveryId) {
    this.deliveryId = deliveryId;
  }

  public void updateOrderRequest(String request) {
    this.request = request;
  }

  public void cancel() {
    if (this.status != OrderStatus.PENDING) {
      throw new BaseException(OrderDomainErrorCode.INVALID_STATUS_TRANSITION);
    }
    this.status = OrderStatus.CANCELLED;
  }

  private int calculateTotalPrice() {
    return orderProducts.stream()
        .reduce(
            0,
            (acc, op) -> {
              int current = Math.multiplyExact(
                  op.getUnitPrice(),
                  op.getQuantity()
              );
              return Math.addExact(acc, current);
            },
            Integer::sum
        );
  }

  private static void validateOrderProducts(List<OrderProduct> orderProducts) {
    if (orderProducts == null || orderProducts.isEmpty())
      throw new BaseException(OrderDomainErrorCode.EMPTY_ORDER_PRODUCT);
  }

  private static void validateCompanyId(UUID companyId) {
    if (companyId == null)
      throw new BaseException(OrderDomainErrorCode.INVALID_COMPANY_ID);
  }

  private static void validateHubId(UUID hubId) {
    if (hubId == null)
      throw new BaseException(OrderDomainErrorCode.MISSING_HUB_ID);
  }
}