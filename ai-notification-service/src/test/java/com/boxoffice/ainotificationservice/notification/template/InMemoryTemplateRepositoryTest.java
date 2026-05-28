package com.boxoffice.ainotificationservice.notification.template;

import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryTemplateRepository")
class InMemoryTemplateRepositoryTest {

    private final InMemoryTemplateRepository repository = new InMemoryTemplateRepository();

    @Test
    @DisplayName("M1 권장 4종 템플릿 모두 본문 등록되어 있음")
    void m1_templates_registered() {
        assertThat(repository.findBody(TemplateType.MASTER_SIGNUP_REQUEST)).isPresent();
        assertThat(repository.findBody(TemplateType.USER_APPROVED)).isPresent();
        assertThat(repository.findBody(TemplateType.USER_REJECTED)).isPresent();
        assertThat(repository.findBody(TemplateType.ORDER_CANCELED)).isPresent();
    }
}
