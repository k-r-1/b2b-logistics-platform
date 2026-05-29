package com.boxoffice.companyservice.company.dto.response;

import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CompanyCreateResponseDto {

    private UUID companyId;
    private String name;
    private CompanyType type;
    private UUID hubId;
    private AddressResponseDto address;

    public static CompanyCreateResponseDto from(Company company) {
        return new CompanyCreateResponseDto(
                company.getId(),
                company.getName(),
                company.getType(),
                company.getHubId(),
                AddressResponseDto.from(company.getAddress())
        );
    }
}
