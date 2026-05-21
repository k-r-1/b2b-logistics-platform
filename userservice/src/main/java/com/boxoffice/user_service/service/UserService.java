package com.boxoffice.user_service.service;

import com.boxoffice.common.exception.BaseException; // 팀원분의 BaseException 임포트
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.user_service.client.KeycloakClient;
import com.boxoffice.user_service.client.KeycloakUserCreateRequestDto;
import com.boxoffice.user_service.dto.UserLoginRequestDto;
import com.boxoffice.user_service.dto.UserResponseDto;
import com.boxoffice.user_service.dto.UserSignupRequestDto;
import com.boxoffice.user_service.dto.UserStatusUpdateRequestDto;
import com.boxoffice.user_service.entity.Email;
import com.boxoffice.user_service.entity.User;
import com.boxoffice.user_service.entity.UserStatus;
import com.boxoffice.user_service.exception.UserErrorCode; // 하단에 추가할 ErrorCode
import com.boxoffice.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

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

    @Value("${keycloak.client-id:boxoffice-app}")
    private String userClientId; // 우리가 Keycloak에 만든 Public 클라이언트 ID

    @Transactional
    public void signUp(UserSignupRequestDto request) {
        // 1. 중복 이메일 검증
        if (userRepository.findByEmailValue(request.getEmail()).isPresent()) {
            log.warn("[Signup] 중복 회원가입 시도 차단. Email: {}", request.getEmail());
            throw new BaseException(UserErrorCode.DUPLICATE_EMAIL);
        }

        String adminAccessToken = getKeycloakAdminToken();

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

        if (response.getStatusCode().is2xxSuccessful()) {
            String keycloakSub = extractKeycloakSub(response);

            Email emailVo = new Email(request.getEmail());
            User user = User.builder()
                    .keycloakSub(keycloakSub)
                    .email(emailVo)
                    .name(request.getName())
                    .role(request.getRole())
                    .status(UserStatus.PENDING)
                    .hubId(request.getHubId())
                    .build();

            userRepository.save(user);

            try {
                userRepository.save(user);
                log.info("[Signup] Keycloak 및 DB 유저 생성 완료. Email: {}", request.getEmail());
            } catch (Exception e) {
                // DB 저장이 실패했다면, Keycloak에는 유저가 만들어진 채로 붕 뜨게 됩니다.
                log.error("[CRITICAL_ALERT] DB 저장 실패로 인한 고아 데이터 발생! Keycloak 유저 수동 삭제 필요. Sub: {}, Email: {}", keycloakSub, request.getEmail(), e);

                // TODO (선택사항): 나중에 keycloakClient.deleteUser(adminAccessToken, realm, keycloakSub) API를 뚫어서

                throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR);
            }
        } else {
            // Keycloak 생성 실패 시
            log.error("[Signup] Keycloak 유저 생성 실패. Email: {}, StatusCode: {}", request.getEmail(), response.getStatusCode());
            throw new BaseException(UserErrorCode.KEYCLOAK_USER_CREATE_FAILED);
        }
    }

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
            log.error("[Keycloak] Admin 토큰 발급 과정에서 치명적 에러 발생: {}", e.getMessage(), e);
            throw new BaseException(UserErrorCode.KEYCLOAK_ADMIN_AUTH_FAILED);
        }
    }

    private String extractKeycloakSub(ResponseEntity<Void> response) {
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            log.error("[Keycloak] 유저 생성 응답에 Location 헤더가 누락되었습니다.");
            throw new BaseException(UserErrorCode.KEYCLOAK_LOCATION_HEADER_MISSING);
        }

        String path = location.getPath();
        return path.substring(path.lastIndexOf("/") + 1);
    }

    /**
     * 🌟 일반 유저 로그인 로직 (토큰 대리 발급)
     */
    public String login(UserLoginRequestDto request) {
        // 1. Keycloak에 던질 로그인 폼 데이터 조립
        Map<String, String> formParams = new HashMap<>();
        formParams.put("client_id", userClientId); // "boxoffice-app"
        formParams.put("grant_type", "password");
        formParams.put("username", request.getUsername());
        formParams.put("password", request.getPassword());

        try {
            // 2. Keycloak 토큰 발급 API 호출 (관리자 토큰 발급 메서드와 엔드포인트가 완벽히 동일하므로 재활용)
            Map<String, Object> tokenResponse = keycloakClient.getAdminToken(realm, formParams);

            // 3. 발급된 access_token 추출하여 반환
            String accessToken = (String) tokenResponse.get("access_token");
            log.info("[Login] 로그인 성공. 토큰 발급 완료. Username: {}", request.getUsername());
            return accessToken;

        } catch (Exception e) {
            // 4. 비밀번호가 틀렸거나 없는 계정이면 Keycloak이 에러를 뱉음
            log.warn("[Login] 로그인 실패 (자격증명 오류). Username: {}", request.getUsername());
            throw new BaseException(UserErrorCode.INVALID_CREDENTIALS);
        }
    }

    /**
     * 🌟 내 정보 조회 (Gateway 헤더 기반)
     */
    @Transactional(readOnly = true)
    public UserResponseDto getMyInfo(String keycloakSub) {
        User user = userRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> {
                    log.error("[UserSearch] 존재하지 않는 유저 조회 시도. Sub: {}", keycloakSub);
                    return new BaseException(UserErrorCode.USER_NOT_FOUND);
                });

        return UserResponseDto.from(user);
    }

    /**
     * 🌟 사용자 목록 검색 (권한별 데이터 격리 + 페이징 적용)
     */
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUserList(String requesterSub, Pageable pageable) {

        // 1. 게이트웨이가 넘겨준 UUID로 '요청을 보낸 사람'의 정보를 찾습니다.
        User requester = userRepository.findByKeycloakSub(requesterSub)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

        Page<User> userPage;

        // 2. 권한(Role)에 따른 분기 처리 (데이터 격리)
        String roleName = requester.getRole().name();

        if ("MASTER".equals(roleName)) {
            // 마스터: 제한 없이 모든 유저 조회
            userPage = userRepository.findAll(pageable);
            log.info("[UserSearch] MASTER 권한으로 전체 유저 목록 조회");

        } else if ("HUB_MANAGER".equals(roleName)) {
            // 허브 매니저: 본인이 소속된 허브(HubId)의 유저만 조회
            userPage = userRepository.findByHubId(requester.getHubId(), pageable);
            log.info("[UserSearch] HUB_MANAGER 권한으로 {} 허브 유저 목록 조회", requester.getHubId());

        } else {
            // 배송 기사 등 다른 권한은 목록 조회 불가 (403 Forbidden)
            log.warn("[UserSearch] 권한 없는 유저의 목록 조회 시도. Sub: {}", requesterSub);
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        // 3. DTO 변환 후 반환
        return userPage.map(UserResponseDto::from);
    }
    /**
     * 🌟 가입 승인/거절 (상태 변경)
     */
    @Transactional
    public UserResponseDto updateUserStatus(UUID targetUserId, String requesterSub, UserStatusUpdateRequestDto request) {

        User requester = userRepository.findByKeycloakSub(requesterSub)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

        String roleName = requester.getRole().name();

        if ("HUB_MANAGER".equals(roleName)) {
            // 허브 매니저라면, 대상자가 '자신과 같은 허브' 소속일 때만 승인 가능
            if (!requester.getHubId().equals(targetUser.getHubId())) {
                log.warn("[UserStatus] 권한 없음: 다른 허브 소속 유저 상태 변경 시도. Requester: {}, TargetHub: {}", requester.getHubId(), targetUser.getHubId());
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        } else if (!"MASTER".equals(roleName)) {
            // 마스터나 허브 매니저가 아니면 접근 불가
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        UserStatus newStatus = UserStatus.valueOf(request.getStatus().toUpperCase());
        targetUser.updateStatus(newStatus);

        log.info("[UserStatus] 유저 상태 변경 완료. TargetUserId: {}, NewStatus: {}", targetUserId, newStatus);

        return UserResponseDto.from(targetUser);
    }

    /**
     * 🌟 사용자 삭제 (논리적 삭제 - Soft Delete)
     */
    @Transactional
    public void deleteUser(UUID targetUserId, String requesterSub) {

        User requester = userRepository.findByKeycloakSub(requesterSub)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

        if (!"MASTER".equals(requester.getRole().name())) {
            log.warn("[UserDelete] 권한 없음: MASTER가 아닌 유저가 삭제 시도. RequesterSub: {}", requesterSub);
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        targetUser.softDelete(requester.getId());

        targetUser.updateStatus(UserStatus.DELETED);

        log.info("[UserDelete] 유저 논리적 삭제 완료. TargetUserId: {}", targetUserId);
    }
}