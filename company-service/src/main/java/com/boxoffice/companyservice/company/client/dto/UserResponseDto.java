package com.boxoffice.companyservice.company.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class UserResponseDto {

    private UUID id;
    private String email;
    private String name;
    private String role;
    private UUID hubId;
    private String status;
    private UUID companyId;
}
