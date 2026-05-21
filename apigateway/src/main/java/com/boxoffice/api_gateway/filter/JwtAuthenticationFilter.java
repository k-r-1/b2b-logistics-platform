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

            String authHeader = request.getHeaders().getFirst("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // TODO: Keycloak 또는 JWT 유효성 검증 및 Redis 블랙리스트 체크 로직 들어갈 자리
                // (나중에 토큰 라이브러리를 통해 claims.get("hub_id") 형태로 파싱하게 됩니다.)

                // Keycloak 토큰에서 꺼내올 파싱 결과값 예시
                String userId = "user-uuid-1234";
                String username = "hub_manager_jun";
                String userRole = "HUB_MANAGER";
                String hubId = "hub-uuid-7777";

                ServerHttpRequest.Builder requestBuilder = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Username", username)
                        .header("X-User-Role", userRole);

                if ("HUB_MANAGER".equals(userRole) && hubId != null) {
                    requestBuilder.header("X-User-Hub-Id", hubId);
                }

                ServerHttpRequest mutatedRequest = requestBuilder.build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }

            return chain.filter(exchange);
        };
    }
}