package com.boxoffice.deliverymanagerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class DeliveryAssignResponseDto {
    private UUID deliveryManagerId;
}