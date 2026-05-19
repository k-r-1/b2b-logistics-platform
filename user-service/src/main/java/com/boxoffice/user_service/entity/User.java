package com.boxoffice.user_service.entity;

import com.boxoffice.user_service.common.IdGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "p_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// ★ BaseEntity 상속 추가
public class User extends BaseEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "keycloak_sub", nullable = false, unique = true)
    private String keycloakSub;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = IdGenerator.createUUIDv7();
        }
    }

    @Builder
    public User(String keycloakSub, String email, String name, UserRole role) {
        this.keycloakSub = keycloakSub;
        this.email = email;
        this.name = name;
        this.role = role;
    }
}