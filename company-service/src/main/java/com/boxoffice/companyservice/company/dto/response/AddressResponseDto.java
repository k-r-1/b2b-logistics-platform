package com.boxoffice.companyservice.company.dto.response;

import com.boxoffice.common.entity.AddressVO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressResponseDto {

    private String zipCode;
    private String address;
    private String detailAddress;

    public static AddressResponseDto from(AddressVO address) {
        return new AddressResponseDto(
                address.getZipCode(),
                address.getAddress(),
                address.getDetailAddress()
        );
    }
}
