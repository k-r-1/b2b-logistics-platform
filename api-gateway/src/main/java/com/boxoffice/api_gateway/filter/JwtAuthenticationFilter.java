package com.boxoffice.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    public static class Config {
        // 설정이 필요한 경우 필드 추가
    }

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. 헤더에서 Authorization (JWT) 토큰 추출
            String authHeader = request.getHeaders().getFirst("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // TODO: Keycloak 또는 JWT 유효성 검증 및 Redis 블랙리스트 체크 로직 들어갈 자리

                // 가짜 예시 데이터 (나중에 토큰 파싱 결과값으로 대체)
                String userId = "user-uuid-1234";
                String username = "logistics_master";
                String userRole = "MASTER";

                // 2. 핵심 MSA 패턴: 인증된 유저 정보를 헤더에 담아 내부 서비스로 전달
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Username", username)
                        .header("X-User-Role", userRole)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }

            return chain.filter(exchange);
        };
    }
}