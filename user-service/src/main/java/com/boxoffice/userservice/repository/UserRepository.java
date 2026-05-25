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

    Optional<User> findByKeycloakSub(String keycloakSub);

    Optional<User> findByEmailValue(String email);

    Page<User> findAll(Pageable pageable);

    Page<User> findByHubId(String hubId, Pageable pageable);
}