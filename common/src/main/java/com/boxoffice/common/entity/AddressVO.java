package com.boxoffice.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 공통 주소 Value Object.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AddressVO {
    /** 우편번호. 없는 경우 null. */
    @Size(max = 10, message = "우편번호는 10자를 초과할 수 없습니다.")
    @Column(name = "zip_code")
    private String zipCode;

    /** 도로명 주소. 필수. 최대 255자. */
    @NotBlank(message = "주소는 필수입니다.")
    @Size(max = 255, message = "주소는 255자를 초과할 수 없습니다.")
    @Column(name = "address", nullable = false)
    private String address;

    /** 상세 주소. 없는 경우 null. */
    @Size(max = 255, message = "상세 주소는 255자를 초과할 수 없습니다.")
    @Column(name = "detail_address")
    private String detailAddress;
}