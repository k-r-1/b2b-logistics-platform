package com.boxoffice.ainotificationservice.notification.entity.message;

// 알림 본문 템플릿 식별자. 본문은 TemplateRepository가 관리.
public enum TemplateType {
    MASTER_SIGNUP_REQUEST,
    USER_APPROVED,
    USER_REJECTED,
    ORDER_CANCELED
}
