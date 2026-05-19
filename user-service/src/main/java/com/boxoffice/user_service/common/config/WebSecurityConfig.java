package com.boxoffice.user_service.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // 스프링 시큐리티 웹 보안 활성화
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 보호 기능 비활성화 (REST API 환경이므로 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 회원가입이나 로그인 관련 인증 API 경로는 토큰 없이도 통과시킵니다.
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // 그 외 모든 내부 API 요청은 유효한 JWT 토큰이 있어야만 접근 가능합니다.
                        .anyRequest().authenticated()
                )

                // 3. ★ 핵심: 이 서버를 OAuth2 Resource Server로 지정하고 JWT 토큰 검증을 활성화합니다.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }
}