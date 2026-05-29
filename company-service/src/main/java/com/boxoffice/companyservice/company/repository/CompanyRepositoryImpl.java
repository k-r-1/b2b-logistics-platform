package com.boxoffice.companyservice.company.repository;

import com.boxoffice.companyservice.company.dto.search.CompanySearchCondition;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

import static com.boxoffice.companyservice.company.entity.QCompany.company;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Company> searchCompanies(CompanySearchCondition condition, CompanyType type, Pageable pageable) {
        
        // 방어적 코드: condition이 null로 넘어올 경우 빈 객체로 초기화하여 NPE 방지
        if (condition == null) {
            condition = new CompanySearchCondition();
        }

        // 1. 데이터(Content) 조회 쿼리를 생성한다.
        JPAQuery<Company> contentQuery = queryFactory
                .selectFrom(company)
                .where(
                        containsName(condition.getName()),
                        eqType(type),
                        eqHubId(condition.getHubId())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // 2. 동적 정렬(Sort)을 적용한다. (화이트리스트 기반 안전한 정렬)
        boolean isSorted = false;
        for (Sort.Order o : pageable.getSort()) {
            Order direction = o.isAscending() ? Order.ASC : Order.DESC;
            
            switch (o.getProperty()) {
                case "createdAt":
                    contentQuery.orderBy(new OrderSpecifier<>(direction, company.createdAt));
                    isSorted = true;
                    break;
                case "updatedAt":
                    contentQuery.orderBy(new OrderSpecifier<>(direction, company.updatedAt));
                    isSorted = true;
                    break;
                case "name":
                    contentQuery.orderBy(new OrderSpecifier<>(direction, company.name));
                    isSorted = true;
                    break;
                default:
                    // 허용되지 않은 필드는 무시하여 런타임 에러를 방지한다.
                    break;
            }
        }

        // 유효한 정렬 기준이 적용되지 않았다면 요구사항에 따라 기본 정렬(생성일 역순)을 추가한다.
        if (!isSorted) {
            contentQuery.orderBy(new OrderSpecifier<>(Order.DESC, company.createdAt));
        }

        // 3. 카운트(Count) 조회 쿼리를 분리한다. (QueryDSL 5.0 표준 방식)
        JPAQuery<Long> countQuery = queryFactory
                .select(company.count())
                .from(company)
                .where(
                        containsName(condition.getName()),
                        eqType(type),
                        eqHubId(condition.getHubId())
                );

        // 4. PageableExecutionUtils를 사용하여 최적화된 Page 객체를 반환한다.
        // fetchOne()의 null 반환 가능성에 대한 방어 로직을 추가한다.
        return PageableExecutionUtils.getPage(
                contentQuery.fetch(), 
                pageable, 
                () -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L)
        );
    }

    private BooleanExpression containsName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return company.name.containsIgnoreCase(name);
    }

    private BooleanExpression eqType(CompanyType type) {
        if (type == null) {
            return null;
        }
        return company.type.eq(type);
    }

    private BooleanExpression eqHubId(UUID hubId) {
        if (hubId == null) {
            return null;
        }
        return company.hubId.eq(hubId);
    }
}
