package com.boxoffice.companyservice.company.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserResponseWrapperDto {

    private int status;
    private String message;
    private UserResponseDto data;
}
