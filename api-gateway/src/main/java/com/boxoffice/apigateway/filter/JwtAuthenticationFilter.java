package com.boxoffice.apigateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class Config { }

    public JwtAuthenticationFilter(ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                return redisTemplate.hasKey(token)
                        .flatMap(isBlacklisted -> {

                            if (Boolean.TRUE.equals(isBlacklisted)) {
                                log.warn("[Gateway] 로그아웃된 토큰(블랙리스트)으로 접근 시도 차단 완료!");
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return exchange.getResponse().setComplete();
                            }

                            try {
                                String[] chunks = token.split("\\.");
                                if (chunks.length != 3) {
                                    throw new IllegalArgumentException("유효하지 않은 JWT 형식입니다.");
                                }

                                String payload = new String(Base64.getUrlDecoder().decode(chunks[1]));
                                JsonNode jsonNode = objectMapper.readTree(payload);

                                String userId = jsonNode.path("sub").asText();
                                String username = jsonNode.path("preferred_username").asText();

                                String role = jsonNode.path("role").asText("");
                                String hubId = jsonNode.path("hub_id").asText("");

                                log.info("[Gateway] 토큰 해독 성공. 뒷단 마이크로서비스로 헤더 전파. UserId: {}", userId);

                                ServerHttpRequest.Builder requestBuilder = request.mutate()
                                        .header("X-User-Id", userId)
                                        .header("X-User-Username", username);

                                if (!role.isEmpty()) {
                                    requestBuilder.header("X-User-Role", role);
                                }
                                if (!hubId.isEmpty()) {
                                    requestBuilder.header("X-User-Hub-Id", hubId);
                                }

                                ServerHttpRequest mutatedRequest = requestBuilder.build();

                                return chain.filter(exchange.mutate().request(mutatedRequest).build());

                            } catch (Exception e) {
                                log.error("[Gateway] 토큰 디코딩 중 에러: {}", e.getMessage());
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return exchange.getResponse().setComplete();
                            }
                        });
            }

            return chain.filter(exchange);
        };
    }
}