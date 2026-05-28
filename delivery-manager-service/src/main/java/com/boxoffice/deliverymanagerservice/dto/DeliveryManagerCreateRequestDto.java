package com.boxoffice.deliverymanagerservice.dto;

import com.boxoffice.deliverymanagerservice.entity.DeliveryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class DeliveryManagerCreateRequestDto {
    private UUID userId;        // 배송 담당자로 지정할 유저의 ID
    private UUID hubId;         // 소속될 허브 ID
    private DeliveryType type;  // HUB_TO_HUB 또는 HUB_TO_COMPANY
}