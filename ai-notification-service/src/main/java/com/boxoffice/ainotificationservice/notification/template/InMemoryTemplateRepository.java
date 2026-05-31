package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

// 관리자 수정 API/외부 저장소 도입 전까지의 placeholder 구현.
@Component
public class InMemoryTemplateRepository implements TemplateRepository {

    private static final Map<TemplateType, String> TEMPLATES = Map.of(
            TemplateType.MASTER_SIGNUP_REQUEST, "신규 가입 요청: {name} ({email}), 역할: {role}",
            TemplateType.USER_APPROVED, "안녕하세요 {name}님, 가입이 승인되었습니다.",
            TemplateType.USER_REJECTED, "안녕하세요 {name}님, 가입이 거절되었습니다. 사유: {reason}",
            TemplateType.ORDER_CANCELED,
            "주문 #{orderId}이(가) 취소되었습니다. (주문자: {ordererName}, 허브관리자: {hubManagerName}) 사유: {reason}",
            TemplateType.DELIVERY_STATUS, "배송 #{deliveryId} (주문 #{orderId}) {statusText}. {detail}",
            TemplateType.DISPATCH_DEADLINE,
            "{agentName}님, 주문 #{orderId}의 발송 시한은 {deadline} 입니다. (사유: {reasoning})"
    );

    @Override
    public Optional<String> findBody(TemplateType type) {
        return Optional.ofNullable(TEMPLATES.get(type));
    }
}
