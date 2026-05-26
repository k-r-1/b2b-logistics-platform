package com.boxoffice.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Auditing에서 현재 사용자를 추출하는 구현체.
 *
 * Gateway가 JWT 검증 후 X-User-Id 헤더로 userId를 전달하면
 * 해당 값을 UUID로 변환하여 created_by, updated_by 필드에 자동 세팅한다.
 *
 * TODO: Keycloak + Spring Security 설정 완성 후
 *       SecurityContext 방식으로 변경 여부 검토 필요.
 */
@Slf4j
public class AuditorAwareImpl implements AuditorAware<UUID> {

    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * 현재 인증된 사용자 UUID를 반환한다.
     * Gateway에서 주입한 X-User-Id 헤더에서 추출한다.
     * 헤더가 없거나 UUID 파싱 실패 시 Optional.empty()를 반환한다.
     *
     * @return 현재 사용자 UUID
     */
    @Override
    public Optional<UUID> getCurrentAuditor() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            String userId = attrs.getRequest().getHeader(USER_ID_HEADER);
            if (userId == null) return Optional.empty();

            try {
                return Optional.of(UUID.fromString(userId));
            } catch (IllegalArgumentException e) {
                log.warn("[AuditorAware] Invalid UUID format: {}", userId);
                return Optional.empty();
            }

        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }
}