package com.boxoffice.userservice.dto;

import com.boxoffice.userservice.entity.User;
import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
public class UserResponseDto {
    private UUID id;
    private String email;
    private String name;
    private String role;
    private UUID hubId;
    private String status;

    private UUID companyId;

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail().getValue())
                .name(user.getName())
                .role(user.getRole().name())
                .hubId(user.getHubId())
                .companyId(user.getCompanyId())
                .status(user.getStatus().name())
                .build();
    }
}