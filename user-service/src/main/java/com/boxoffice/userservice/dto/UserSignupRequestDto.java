package com.boxoffice.userservice.dto;

import com.boxoffice.userservice.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSignupRequestDto {

    @NotBlank(message = "로그인에 사용할 사용자 ID는 필수입니다.")
    @Size(min = 4, max = 20, message = "사용자 ID는 4자 이상 20자 이하로 입력해 주세요.")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotNull(message = "역할(Role)은 필수입니다.")
    private UserRole role;

    // (일반 유저는 null 가능)
    private String hubId;
}