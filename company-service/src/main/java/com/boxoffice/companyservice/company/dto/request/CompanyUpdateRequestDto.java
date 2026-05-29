package com.boxoffice.companyservice.company.dto.request;

import com.boxoffice.companyservice.company.entity.CompanyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyUpdateRequestDto {

    @Size(max = 255, message = "업체명은 255자를 초과할 수 없습니다.")
    private String name;

    private CompanyType type;

    @Valid
    private AddressRequestDto address;

    public boolean hasUpdateField() {
        return name != null || type != null || address != null;
    }

    public boolean hasBlankName() {
        return name != null && name.isBlank();
    }
}
