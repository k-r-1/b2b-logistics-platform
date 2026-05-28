package com.boxoffice.deliverymanagerservice.dto;

import com.boxoffice.deliverymanagerservice.entity.DeliveryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class DeliveryAssignRequestDto {
    private UUID hubId;
    private DeliveryType type;
}