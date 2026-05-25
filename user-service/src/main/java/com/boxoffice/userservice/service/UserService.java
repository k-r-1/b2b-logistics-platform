package com.boxoffice.userservice.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.userservice.client.KeycloakClient;
import com.boxoffice.userservice.client.KeycloakUserCreateRequestDto;
import com.boxoffice.userservice.dto.UserLoginRequestDto;
import com.boxoffice.userservice.dto.UserResponseDto;
import com.boxoffice.userservice.dto.UserSignupRequestDto;
import com.boxoffice.userservice.dto.UserStatusUpdateRequestDto;
import com.boxoffice.userservice.entity.Email;
import com.boxoffice.userservice.entity.User;
import com.boxoffice.userservice.entity.UserRole;
import com.boxoffice.userservice.entity.UserStatus;
import com.boxoffice.userservice.exception.UserErrorCode;
import com.boxoffice.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.util.Base64;
import java.util.UUID;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private String userClientId;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void signUp(UserSignupRequestDto request) {
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

            try {
                userRepository.save(user);
                log.info("[Signup] Keycloak 및 DB 유저 생성 완료. Email: {}", request.getEmail());
            } catch (Exception e) {
                log.error("[CRITICAL_ALERT] DB 저장 실패로 인한 고아 데이터 발생! Keycloak 유저 수동 삭제 필요. Sub: {}, Email: {}", keycloakSub, request.getEmail(), e);
                throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR);
            }
        } else {
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
        Map<String, String> formParams = new HashMap<>();
        formParams.put("client_id", userClientId);
        formParams.put("grant_type", "password");
        formParams.put("username", request.getUsername());
        formParams.put("password", request.getPassword());

        try {
            Map<String, Object> tokenResponse = keycloakClient.getAdminToken(realm, formParams);
            String accessToken = (String) tokenResponse.get("access_token");
            log.info("[Login] 로그인 성공. 토큰 발급 완료. Username: {}", request.getUsername());
            return accessToken;
        } catch (Exception e) {
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

        User requester = userRepository.findByKeycloakSub(requesterSub)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

        Page<User> userPage;

        UserRole role = requester.getRole();

        if (role == UserRole.MASTER) {
            userPage = userRepository.findAll(pageable);
            log.info("[UserSearch] MASTER 권한으로 전체 유저 목록 조회");
        } else if (role == UserRole.HUB_MANAGER) {
            userPage = userRepository.findByHubId(requester.getHubId(), pageable);
            log.info("[UserSearch] HUB_MANAGER 권한으로 {} 허브 유저 목록 조회", requester.getHubId());
        } else {
            log.warn("[UserSearch] 권한 없는 유저의 목록 조회 시도. Sub: {}", requesterSub);
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

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

        UserRole role = requester.getRole();

        if (role == UserRole.HUB_MANAGER) {
            if (!requester.getHubId().equals(targetUser.getHubId())) {
                log.warn("[UserStatus] 권한 없음: 다른 허브 소속 유저 상태 변경 시도. Requester: {}, TargetHub: {}", requester.getHubId(), targetUser.getHubId());
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        } else if (role != UserRole.MASTER) {
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

        if (requester.getRole() != UserRole.MASTER) {
            log.warn("[UserDelete] 권한 없음: MASTER가 아닌 유저가 삭제 시도. RequesterSub: {}", requesterSub);
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        targetUser.softDelete(requester.getId());
        targetUser.updateStatus(UserStatus.DELETED);

        log.info("[UserDelete] 유저 논리적 삭제 완료. TargetUserId: {}", targetUserId);
    }

    /**
     * 🌟 사용자 로그아웃 (Redis 블랙리스트 등록)
     */
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
        String token = authHeader.substring(7);

        try {
            String[] chunks = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(chunks[1]));
            JsonNode jsonNode = objectMapper.readTree(payload);

            long exp = jsonNode.path("exp").asLong() * 1000;
            long now = System.currentTimeMillis();
            long remainingTime = exp - now;

            if (remainingTime > 0) {
                redisTemplate.opsForValue().set(token, "logout", remainingTime, TimeUnit.MILLISECONDS);
                log.info("[Logout] 토큰이 Redis 블랙리스트에 등록되었습니다. 남은 수명: {}ms", remainingTime);
            }
        } catch (Exception e) {
            log.error("[Logout] 토큰 파싱 에러 발생: {}", e.getMessage());
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 🌟 외부 마이크로서비스(주문 등) 통신용 유저 단건 조회 (ID 기반)
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
        return UserResponseDto.from(user);
    }

    /**
     * 🌟 [Internal] 타 서비스용 유저 단건 조회 (Keycloak Sub 기반)
     * 다른 서비스가 Gateway에서 전달받은 X-User-Id(sub) 값을 그대로 사용하여 조회할 수 있게 합니다.
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserBySub(String keycloakSub) {
        User user = userRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> {
                    log.error("[Internal Search] 존재하지 않는 Keycloak Sub 조회 시도. Sub: {}", keycloakSub);
                    return new BaseException(UserErrorCode.USER_NOT_FOUND);
                });
        return UserResponseDto.from(user);
    }
}