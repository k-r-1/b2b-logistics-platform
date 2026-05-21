package com.boxoffice.user_service.repository;

import com.boxoffice.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 🌟 Keycloak의 고유 식별자(sub)로 우리 DB의 유저를 찾습니다.
     * 회원가입 시 중복 가입 방지 및 로그인 유저 정보 조회 시 핵심 기둥이 되는 메서드입니다.
     */
    Optional<User> findByKeycloakSub(String keycloakSub);

    /**
     * 이메일 값 객체(VO) 내부의 value 문자열을 조건으로 유저를 찾습니다.
     * 중복 이메일 가입을 막기 위해 필요합니다.
     */
    Optional<User> findByEmailValue(String email);
}