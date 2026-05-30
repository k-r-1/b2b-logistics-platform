package com.boxoffice.deliverymanagerservice.dto;

import com.boxoffice.deliverymanagerservice.entity.DeliveryType;
import com.boxoffice.deliverymanagerservice.entity.ManagerStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeliveryManagerSearchDto {
    private UUID hubId;
    private DeliveryType type;
    private ManagerStatus status;
}