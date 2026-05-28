package com.boxoffice.companyservice.company.dto.request;

import com.boxoffice.companyservice.company.entity.CompanyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class CompanyCreateRequestDto {

    @NotBlank(message = "업체명은 필수입니다.")
    private String name;

    @NotNull(message = "업체 타입은 필수입니다.")
    private CompanyType type;

    @NotNull(message = "허브 ID는 필수입니다.")
    private UUID hubId;

    private UUID managerUserId;

    @Valid
    @NotNull(message = "업체 주소는 필수입니다.")
    private AddressRequestDto address;
}
