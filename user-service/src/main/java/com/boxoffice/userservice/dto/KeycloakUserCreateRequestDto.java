package com.boxoffice.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Collections;
import java.util.List;

@Getter
public class KeycloakUserCreateRequestDto {
    private final String username;
    private final String email;
    private final boolean enabled = true;
    private final String firstName;
    private final String lastName;
    private final List<Credential> credentials;

    public KeycloakUserCreateRequestDto(String username, String email, String password, String name) {
        this.username = username;
        this.email = email;
        this.firstName = name;
        this.lastName = "";
        this.credentials = Collections.singletonList(new Credential(password));
    }

    @Getter
    @AllArgsConstructor
    public static class Credential {
        private final String type = "password";
        private final String value;
        private final boolean temporary = false;
    }
}