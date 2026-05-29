package com.boxoffice.userservice.repository;

import com.boxoffice.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakSub(String keycloakSub);

    Optional<User> findByEmailValue(String email);

    Page<User> findAll(Pageable pageable);

    Page<User> findByHubId(UUID hubId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.hubId = null WHERE u.hubId = :hubId")
    void clearHubIdByHubId(@Param("hubId") UUID hubId);
}