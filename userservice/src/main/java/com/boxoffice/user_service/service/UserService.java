package com.boxoffice.user_service.service;

import com.boxoffice.user_service.client.KeycloakClient;
import com.boxoffice.user_service.client.KeycloakUserCreateRequestDto;
import com.boxoffice.user_service.dto.UserSignupRequestDto;
import com.boxoffice.user_service.entity.Email;
import com.boxoffice.user_service.entity.User;
import com.boxoffice.user_service.entity.UserStatus;
import com.boxoffice.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakClient keycloakClient;

    @Value("${keycloak.realm:boxoffice-realm}")
    private String realm;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:boxoffice123}")
    private String adminPassword;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String adminClientId;

    /**
     * 🌟 대망의 회원가입 전체 비즈니스 오케스트레이션 로직
     */
    @Transactional
    public void signUp(UserSignupRequestDto request) {
        // 1. 우리 서비스 DB 내부 중복 검사 (이메일 검증)
        if (userRepository.findByEmailValue(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일 주소입니다.");
        }

        // 2. Keycloak 문을 열기 위한 관리자(Admin) Access Token 획득
        String adminAccessToken = getKeycloakAdminToken();

        // 3. Keycloak용 요청 가방 조립 후 계정 생성 호출
        KeycloakUserCreateRequestDto keycloakRequest = new KeycloakUserCreateRequestDto(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getName()
        );

        ResponseEntity<Void> response = keycloakClient.createUser(
                "Bearer " + adminAccessToken,
                realm,
                keycloakRequest
        );

        // 4. Keycloak 생성 결과 분석 및 고유키(sub) 추출
        if (response.getStatusCode().is2xxSuccessful()) {
            String keycloakSub = extractKeycloakSub(response);
            log.info("[Signup] Keycloak 계정 생성 성공. sub: {}", keycloakSub);

            // 5. 값 객체(Email VO) 및 엔티티 조립
            Email emailVo = new Email(request.getEmail());
            User user = User.builder()
                    .keycloakSub(keycloakSub)
                    .email(emailVo)
                    .name(request.getName())
                    .role(request.getRole())
                    .status(UserStatus.PENDING) // 기획서대로 가입 직후는 PENDING(대기) 상태!
                    .hubId(request.getHubId())
                    .build();

            // 6. 우리 서비스 PostgreSQL DB에 최종 영속화
            userRepository.save(user);
            log.info("[Signup] 우리 서비스 DB에 유저 정보 저장 성공. ID: {}", user.getName());

        } else {
            log.error("[Signup] Keycloak 유저 생성 실패. Status Code: {}", response.getStatusCode());
            throw new RuntimeException("인증 서버에 사용자 등록을 실패했습니다.");
        }
    }

    /**
     * 🔑 Keycloak 마스터 권한 토큰을 받아오는 내부 헬퍼 메서드
     */
    private String getKeycloakAdminToken() {
        Map<String, String> formParams = new HashMap<>();
        formParams.put("client_id", adminClientId);
        formParams.put("grant_type", "password");
        formParams.put("username", adminUsername);
        formParams.put("password", adminPassword);

        try {
            Map<String, Object> tokenResponse = keycloakClient.getAdminToken(realm, formParams);
            return (String) tokenResponse.get("access_token");
        } catch (Exception e) {
            log.error("[Keycloak] Admin 토큰 발급 실패: {}", e.getMessage());
            throw new RuntimeException("인증 서버 관리자 인증에 실패했습니다.");
        }
    }

    /**
     * ✂️ Keycloak 응답 헤더(Location)에서 유저의 고유 UUID(sub)를 찢어내는 칼날 메서드
     */
    private String extractKeycloakSub(ResponseEntity<Void> response) {
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new IllegalStateException("Keycloak 응답에 Location 헤더가 누락되었습니다.");
        }

        String path = location.getPath(); // 예: /admin/realms/boxoffice-realm/users/b7a8d-9999...
        return path.substring(path.lastIndexOf("/") + 1); // 맨 마지막 슬래시 뒤의 UUID만 쏙 추출
    }
}