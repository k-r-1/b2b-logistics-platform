package com.boxoffice.companyservice.company.repository;

import com.boxoffice.companyservice.company.dto.search.CompanySearchCondition;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyRepositoryCustom {

    Page<Company> searchCompanies(CompanySearchCondition condition, CompanyType type, Pageable pageable);
}
