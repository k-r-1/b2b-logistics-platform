package com.boxoffice.deliverymanagerservice.dto;

import com.boxoffice.deliverymanagerservice.entity.DeliveryManager;
import com.boxoffice.deliverymanagerservice.entity.DeliveryType;
import com.boxoffice.deliverymanagerservice.entity.ManagerStatus;
import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
public class DeliveryManagerResponseDto {
    private UUID id;            // 배송 담당자 고유 ID (PK)
    private UUID userId;        // 매핑된 유저 ID
    private UUID hubId;         // 소속 허브 ID
    private DeliveryType type;  // 배송 타입
    private String slackId;
    private ManagerStatus status;

    public static DeliveryManagerResponseDto from(DeliveryManager deliveryManager) {
        return DeliveryManagerResponseDto.builder()
                .id(deliveryManager.getId())
                .userId(deliveryManager.getUserId())
                .hubId(deliveryManager.getHubId())
                .type(deliveryManager.getType())
                .slackId(deliveryManager.getSlackId())
                .status(deliveryManager.getStatus())
                .build();
    }
}