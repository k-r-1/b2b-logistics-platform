package com.boxoffice.user_service.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Collections;
import java.util.List;

@Getter
public class KeycloakUserCreateRequestDto {
    private final String username;
    private final String email;
    private final boolean enabled = true;
    private final String firstName;
    private final String lastName;
    private final List<Credential> credentials;

    public KeycloakUserCreateRequestDto(String username, String email, String password, String name) {
        this.username = username;
        this.email = email;
        // 한국인 이름 특성상 성과 이름을 찢기 모호하므로 firstName에 전체 이름을 다 밀어 넣는 것이 현업 꿀팁입니다.
        this.firstName = name;
        this.lastName = "";
        this.credentials = Collections.singletonList(new Credential(password));
    }

    @Getter
    @AllArgsConstructor
    public static class Credential {
        private final String type = "password";
        private final String value;
        private final boolean temporary = false; // 🌟 아까 짚었던 임시비밀번호 OFF 설정 자동화!
    }
}