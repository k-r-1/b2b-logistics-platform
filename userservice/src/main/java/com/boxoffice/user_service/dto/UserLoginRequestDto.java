package com.boxoffice.user_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserLoginRequestDto {
    private String username; // 포스트맨에서 입력했던 logistics_jun
    private String password; // 포스트맨에서 입력했던 password1234
}