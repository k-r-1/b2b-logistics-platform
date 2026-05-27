package com.boxoffice.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSignupRequest {

    @NotBlank(message = "사용자 ID는 필수 입력 사항입니다.")
    @Pattern(regexp = "^[a-z0-9]{4,10}$",
            message = "username은 최소 4자 이상, 10자 이하이며 알파벳 소문자와 숫자로만 구성되어야 합니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수 입력 사항입니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,15}$",
            message = "password는 최소 8자 이상, 15자 이하이며 알파벳 대소문자, 숫자, 특수문자를 최소 하나씩 포함해야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수 입력 사항입니다.")
    private String nickname;

    @NotBlank(message = "이메일은 필수 입력 사항입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "슬랙 ID는 필수 입력 사항입니다.")
    private String slackId;

    @NotBlank(message = "소속 명칭(업체명 또는 허브명)은 필수 입력 사항입니다.")
    private String companyOrHubName;
}