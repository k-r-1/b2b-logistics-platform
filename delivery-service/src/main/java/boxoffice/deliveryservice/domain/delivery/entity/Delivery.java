package boxoffice.deliveryservice.domain.delivery.entity;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_delivery")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Delivery extends BaseEntity {
    @NotNull
    private UUID orderId;
    @NotNull
    private UUID companyId;
    @NotNull
    private UUID originHubId;
    @NotNull
    private UUID destinationHubId;
    @NotNull
    private AddressVO deliveryAddress;
    private UUID deliveryPersonId;
    @NotBlank
    private String recipientName;
    private String recipientSlackId;
    @NotNull
    @Enumerated(EnumType.STRING)

    private DeliveryStatus deliveryStatus;

    public void assignDeliveryPerson(UUID deliveryPersonId) {
        this.deliveryPersonId = deliveryPersonId;
    }

    public boolean isCanceled() {
        return this.deliveryStatus == DeliveryStatus.CANCELED;
    }

    public void cancel() {
        this.deliveryStatus = DeliveryStatus.CANCELED;
    }

    public void updateInfo(String recipientName, String recipientSlackId, AddressVO deliveryAddress) {
        this.recipientName = recipientName;
        this.recipientSlackId = recipientSlackId;
        this.deliveryAddress = deliveryAddress;
    }

    public void updateStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    // 정적 팩토리 메서드
    public static Delivery create(UUID orderId, UUID companyId, UUID originHubId, UUID destinationHubId,
                                  AddressVO deliveryAddress, String recipientName, String recipientSlackId) {
        return new Delivery(orderId, companyId, originHubId, destinationHubId, deliveryAddress,
                null, recipientName, recipientSlackId, DeliveryStatus.WAITING);
    }
}
