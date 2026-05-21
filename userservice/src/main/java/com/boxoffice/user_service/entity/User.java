package com.boxoffice.user_service.entity;

import com.boxoffice.common.entity.BaseEntity; // 공통 모듈 상속
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "p_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(name = "keycloak_sub", nullable = false, unique = true)
    private String keycloakSub;

    @Embedded
    private Email email;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "hub_id", nullable = true)
    private String hubId;

    @Builder
    public User(String keycloakSub, Email email, String name, UserRole role, UserStatus status, String hubId) {
        this.keycloakSub = keycloakSub;
        this.email = email;
        this.name = name;
        this.role = role;
        this.status = status != null ? status : UserStatus.PENDING;
        this.hubId = hubId;
    }
}