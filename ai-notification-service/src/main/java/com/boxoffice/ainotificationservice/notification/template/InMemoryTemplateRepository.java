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
            TemplateType.ORDER_CANCELED, "주문 #{orderId}이(가) 취소되었습니다. 사유: {reason}"
    );

    @Override
    public Optional<String> findBody(TemplateType type) {
        return Optional.ofNullable(TEMPLATES.get(type));
    }
}
