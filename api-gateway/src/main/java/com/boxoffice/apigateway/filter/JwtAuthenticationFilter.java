package com.boxoffice.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ReactiveJwtDecoder jwtDecoder;

    public static class Config { }

    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars"
    );

    public JwtAuthenticationFilter(ReactiveStringRedisTemplate redisTemplate, ReactiveJwtDecoder jwtDecoder) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            boolean isExcludePath = EXCLUDE_PATHS.stream().anyMatch(path::startsWith);

            if (isExcludePath) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[Gateway] 인증 토큰이 누락되었습니다. 요청 경로: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            return redisTemplate.hasKey(token)
                    .flatMap(isBlacklisted -> {
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            log.warn("[Gateway] 로그아웃된 토큰(블랙리스트)으로 접근 시도 차단 완료!");
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }

                        return jwtDecoder.decode(token)
                                .flatMap(jwt -> {
                                    String userId = jwt.getSubject();
                                    String username = jwt.getClaimAsString("preferred_username");
                                    String hubId = jwt.getClaimAsString("hub_id"); // 커스텀 클레임

                                    String role = "";
                                    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                                    if (realmAccess != null && realmAccess.containsKey("roles")) {
                                        List<String> roles = (List<String>) realmAccess.get("roles");
                                        if (roles != null && !roles.isEmpty()) {
                                            role = roles.get(0);
                                        }
                                    }

                                    log.info("[Gateway] 토큰 서명 검증 성공. UserId: {}, Role: {}", userId, role);

                                    ServerHttpRequest.Builder requestBuilder = request.mutate()
                                            .header("X-User-Id", userId)
                                            .header("X-User-Username", username);

                                    if (!role.isEmpty()) {
                                        requestBuilder.header("X-User-Role", role);
                                    }
                                    if (hubId != null && !hubId.isEmpty()) {
                                        requestBuilder.header("X-User-Hub-Id", hubId);
                                    }

                                    return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
                                })
                                .onErrorResume(e -> {
                                    log.error("[Gateway] 토큰 서명 검증 실패 또는 위조된 토큰: {}", e.getMessage());
                                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                    return exchange.getResponse().setComplete();
                                });
                    });
        };
    }
}