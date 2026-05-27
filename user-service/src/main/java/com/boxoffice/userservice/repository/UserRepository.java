package com.boxoffice.userservice.repository;

import com.boxoffice.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // 🌟 KeycloakSub(UUID)로 유저를 찾는 메서드 추가
    Optional<User> findByKeycloakSub(String keycloakSub);

    /**
     * 이메일 값 객체(VO) 내부의 value 문자열을 조건으로 유저를 찾습니다.
     * 중복 이메일 가입을 막기 위해 필요합니다.
     */
    Optional<User> findByEmailValue(String email);

    // 전체 유저 목록을 페이징하여 가져오는 메서드
    Page<User> findAll(Pageable pageable);

    // 허브 매니저를 위한 '특정 허브 소속 유저만' 가져오는 쿼리
    Page<User> findByHubId(String hubId, Pageable pageable);
}