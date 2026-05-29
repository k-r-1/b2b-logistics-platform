package com.boxoffice.companyservice.company.dto.request;

import com.boxoffice.common.entity.AddressVO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddressRequestDto {

    @Size(max = 10, message = "우편번호는 10자를 초과할 수 없습니다.")
    private String zipCode;

    @NotBlank(message = "주소는 필수입니다.")
    @Size(max = 255, message = "주소는 255자를 초과할 수 없습니다.")
    private String address;

    @Size(max = 255, message = "상세 주소는 255자를 초과할 수 없습니다.")
    private String detailAddress;

    public AddressVO toAddressVO() {
        return new AddressVO(zipCode, address, detailAddress);
    }
}
