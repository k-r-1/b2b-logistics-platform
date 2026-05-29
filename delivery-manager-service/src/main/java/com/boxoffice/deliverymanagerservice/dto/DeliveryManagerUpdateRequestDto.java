package com.boxoffice.deliverymanagerservice.dto;

import com.boxoffice.deliverymanagerservice.entity.DeliveryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class DeliveryManagerUpdateRequestDto {
    private UUID hubId;         // 변경할 소속 허브 ID (Optional)
    private DeliveryType type;  // 변경할 배송 타입 (Optional)
}