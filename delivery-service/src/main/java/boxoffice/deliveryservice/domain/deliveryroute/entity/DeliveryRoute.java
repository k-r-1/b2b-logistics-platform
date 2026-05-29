package boxoffice.deliveryservice.domain.deliveryroute.entity;

import boxoffice.deliveryservice.domain.delivery.entity.Delivery;
import com.boxoffice.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_delivery_routes")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@SQLRestriction("deleted_at IS NULL")
public class DeliveryRoute extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @NotNull
    private UUID originHubId;

    @NotNull
    private UUID destinationHubId;

    private UUID hubDeliveryPersonId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private DeliveryRouteStatus status;

    @NotNull
    private BigDecimal expectedDistance;

    @NotNull
    private Integer expectedDuration;

    private BigDecimal actualDistance;
    private Integer actualDuration;
    private Integer sequence;
}
