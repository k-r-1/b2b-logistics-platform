package com.boxoffice.delivery_manager_service.common.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserContextInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String username = request.getHeader("X-User-Username");
        String role = request.getHeader("X-User-Role");

        if (username != null) UserContextHolder.setUsername(username);
        if (role != null) UserContextHolder.setRole(role);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear(); // 메모리 누수 방지를 위해 요청이 끝나면 청소 필수!
    }
}