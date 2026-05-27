package com.boxoffice.userservice.entity;

import com.boxoffice.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "p_users")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "hub_id", nullable = true)
    private UUID hubId;


    @Column(name = "company_id", nullable = true)
    private UUID companyId;

    @Builder
    public User(String keycloakSub, Email email, String name, UserRole role, UserStatus status, UUID hubId) {
        this.keycloakSub = keycloakSub;
        this.email = email;
        this.name = name;
        this.role = role;
        this.status = status != null ? status : UserStatus.PENDING;
        this.hubId = hubId;
    }

    public void updateStatus(UserStatus newStatus) {
        this.status = newStatus;
    }

    public void updateCompany(UUID companyId) {
        this.companyId = companyId;
    }

    public void updateHub(UUID newHubId) {
        this.hubId = newHubId;
    }
}