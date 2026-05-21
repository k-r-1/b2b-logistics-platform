package com.boxoffice.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. JWT 토큰을 사용하는 무상태(Stateless) API이므로 CSRF 보호는 비활성화합니다.
                .csrf(AbstractHttpConfigurer::disable)

                // 2. URL별 접근 권한(인가) 설정
                .authorizeHttpRequests(auth -> auth
                        // 🌟 핵심: 회원가입이 들어오는 /api/v1/auth/** 경로는 토큰 검사 없이 '전원 통과'시킵니다.
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // 그 외에 /api/v1/users 같은 주소들은 반드시 유효한 JWT 토큰이 있어야만 접근 가능합니다.
                        .anyRequest().authenticated()
                )

                // 3. application.yml에 등록한 issuer-uri 정보를 바탕으로 JWT 토큰 자동 검증 기능을 활성화합니다.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }
}