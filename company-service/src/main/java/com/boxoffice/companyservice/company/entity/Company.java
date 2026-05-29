package com.boxoffice.companyservice.company.entity;

import com.boxoffice.common.entity.BaseEntity;
import com.boxoffice.common.entity.AddressVO;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_companies")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private CompanyType type;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Embedded
    private AddressVO address;

    private Company(String name, CompanyType type, UUID hubId, AddressVO address) {
        this.name = name;
        this.type = type;
        this.hubId = hubId;
        this.address = address;
    }

    public static Company create(String name, CompanyType type, UUID hubId, AddressVO address) {
        return new Company(name, type, hubId, address);
    }
}
