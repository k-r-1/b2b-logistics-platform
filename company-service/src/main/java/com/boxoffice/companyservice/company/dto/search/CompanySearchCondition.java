package com.boxoffice.companyservice.company.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 업체 검색 조건을 담는 DTO.
 * Controller에서 @ModelAttribute로 바인딩되어 Repository까지 전달된다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanySearchCondition {
    
    /** 업체명 (부분 일치 검색) */
    private String name;
    
    /** 업체 타입 (정확히 일치 검색). 프론트엔드 입력값(String). */
    private String type;
    
    /** 소속 허브 ID (정확히 일치 검색) */
    private UUID hubId;
}
