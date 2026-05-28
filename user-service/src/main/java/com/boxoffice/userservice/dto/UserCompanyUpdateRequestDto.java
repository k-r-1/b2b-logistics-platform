package com.boxoffice.userservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class UserCompanyUpdateRequestDto {
    private UUID companyId;
}