package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.common.exception.BaseException;

// 배송 상태 변경 알림. status에 따라 표시 문구·세부정보(수령인/실패사유)가 달라진다.
// recipientName·failureReason은 status에 따라 선택적이며, 없으면 "미상"으로 표기.
public record DeliveryStatusContext(
        String deliveryId,
        String orderId,
        DeliveryStatus status,
        String recipientName,
        String failureReason
) implements TemplateContext {

    public DeliveryStatusContext {
        if (deliveryId == null || deliveryId.isBlank()
                || orderId == null || orderId.isBlank()
                || status == null) {
            throw new BaseException(NotificationErrorCode.TEMPLATE_CONTEXT_MISSING);
        }
    }

    @Override
    public TemplateType type() {
        return TemplateType.DELIVERY_STATUS;
    }

    @Override
    public String render(String bodyTemplate) {
        return bodyTemplate
                .replace("{deliveryId}", deliveryId)
                .replace("{orderId}", orderId)
                .replace("{statusText}", statusText())
                .replace("{detail}", detail());
    }

    private String statusText() {
        return switch (status) {
            case ARRIVED_AT_DESTINATION -> "도착지에 도착했습니다";
            case DELIVERED -> "배송이 완료되었습니다";
            case FAILED -> "배송에 실패했습니다";
        };
    }

    private String detail() {
        return switch (status) {
            case ARRIVED_AT_DESTINATION, DELIVERED -> "수령인: " + (recipientName != null ? recipientName : "미상");
            case FAILED -> "사유: " + (failureReason != null ? failureReason : "미상");
        };
    }
}
