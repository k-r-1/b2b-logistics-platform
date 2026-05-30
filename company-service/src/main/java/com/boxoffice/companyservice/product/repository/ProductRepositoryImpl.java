package com.boxoffice.companyservice.product.repository;

import com.boxoffice.companyservice.product.dto.search.ProductSearchCondition;
import com.boxoffice.companyservice.product.entity.Product;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.boxoffice.companyservice.company.entity.QCompany.company;
import static com.boxoffice.companyservice.product.entity.QProduct.product;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Product> findProduct(UUID companyId, UUID productId) {
        Product result = queryFactory
                .selectFrom(product)
                .join(product.company, company).fetchJoin()
                .where(
                        eqProductId(productId),
                        eqCompanyId(companyId),
                        activeProduct(),
                        activeCompany()
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<Product> searchProducts(UUID companyId, ProductSearchCondition condition, Pageable pageable) {
        ProductSearchCondition validCondition = condition == null ? new ProductSearchCondition() : condition;

        // 응답 DTO에서 companyId/hubId를 바로 사용하므로 Company를 fetch join해 지연 로딩 추가 쿼리를 막는다.
        JPAQuery<Product> contentQuery = queryFactory
                .selectFrom(product)
                .join(product.company, company).fetchJoin()
                .where(searchConditions(companyId, null, validCondition))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        applySort(contentQuery, pageable);

        // count 쿼리는 fetch join 없이 분리해 페이징 조회 비용을 줄인다.
        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .join(product.company, company)
                .where(searchConditions(companyId, null, validCondition));

        return PageableExecutionUtils.getPage(
                contentQuery.fetch(),
                pageable,
                () -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L)
        );
    }

    @Override
    public Page<Product> searchProducts(ProductSearchCondition condition, Pageable pageable) {
        ProductSearchCondition validCondition = condition == null ? new ProductSearchCondition() : condition;

        JPAQuery<Product> contentQuery = queryFactory
                .selectFrom(product)
                .join(product.company, company).fetchJoin()
                .where(searchConditions(validCondition.getCompanyId(), validCondition.getHubId(), validCondition))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        applySort(contentQuery, pageable);

        // count 쿼리는 fetch join 없이 분리해 페이징 조회 비용을 줄인다.
        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .join(product.company, company)
                .where(searchConditions(validCondition.getCompanyId(), validCondition.getHubId(), validCondition));

        return PageableExecutionUtils.getPage(
                contentQuery.fetch(),
                pageable,
                () -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L)
        );
    }

    @Override
    public Map<UUID, Long> sumStockQuantityByHubIds(List<UUID> hubIds) {
        if (hubIds == null || hubIds.isEmpty()) {
            return Map.of();
        }

        // Hub 폐쇄 계획에서 여러 허브 재고를 한 번에 보도록 group by 집계로 N+1 호출을 피한다.
        NumberExpression<Long> stockSum = product.stockQuantity.sum().longValue();

        return queryFactory
                .select(company.hubId, stockSum)
                .from(product)
                .join(product.company, company)
                .where(
                        company.hubId.in(hubIds),
                        activeProduct(),
                        activeCompany()
                )
                .groupBy(company.hubId)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(company.hubId),
                        tuple -> Optional.ofNullable(tuple.get(stockSum)).orElse(0L)
                ));
    }

    @Override
    public Map<UUID, Long> sumStockQuantityByCompanyIds(List<UUID> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Map.of();
        }

        NumberExpression<Long> stockSum = product.stockQuantity.sum().longValue();

        return queryFactory
                .select(company.id, stockSum)
                .from(product)
                .join(product.company, company)
                .where(
                        company.id.in(companyIds),
                        activeProduct(),
                        activeCompany()
                )
                .groupBy(company.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(company.id),
                        tuple -> Optional.ofNullable(tuple.get(stockSum)).orElse(0L)
                ));
    }

    private void applySort(JPAQuery<Product> contentQuery, Pageable pageable) {
        boolean isSorted = false;
        for (Sort.Order o : pageable.getSort()) {
            Order direction = o.isAscending() ? Order.ASC : Order.DESC;

            switch (o.getProperty()) {
                case "createdAt":
                    contentQuery.orderBy(new OrderSpecifier<>(direction, product.createdAt));
                    isSorted = true;
                    break;
                case "updatedAt":
                    contentQuery.orderBy(new OrderSpecifier<>(direction, product.updatedAt));
                    isSorted = true;
                    break;
                case "name":
                    contentQuery.orderBy(new OrderSpecifier<>(direction, product.name));
                    isSorted = true;
                    break;
                case "price":
                    contentQuery.orderBy(new OrderSpecifier<>(direction, product.price.value));
                    isSorted = true;
                    break;
                case "stockQuantity":
                    contentQuery.orderBy(new OrderSpecifier<>(direction, product.stockQuantity));
                    isSorted = true;
                    break;
                default:
                    break;
            }
        }

        if (!isSorted) {
            contentQuery.orderBy(new OrderSpecifier<>(Order.DESC, product.createdAt));
        }
    }

    private Predicate[] searchConditions(UUID companyId, UUID hubId, ProductSearchCondition condition) {
        return new Predicate[]{
                eqCompanyId(companyId),
                eqHubId(hubId),
                activeProduct(),
                activeCompany(),
                containsName(condition.getName()),
                priceGoe(condition.getMinPrice()),
                priceLoe(condition.getMaxPrice()),
                stockQuantityGoe(condition.getMinStockQuantity()),
                stockQuantityLoe(condition.getMaxStockQuantity())
        };
    }

    private BooleanExpression eqProductId(UUID productId) {
        if (productId == null) {
            return null;
        }
        return product.id.eq(productId);
    }

    private BooleanExpression eqCompanyId(UUID companyId) {
        if (companyId == null) {
            return null;
        }
        return company.id.eq(companyId);
    }

    private BooleanExpression eqHubId(UUID hubId) {
        if (hubId == null) {
            return null;
        }
        return company.hubId.eq(hubId);
    }

    private BooleanExpression activeProduct() {
        return product.deletedAt.isNull();
    }

    private BooleanExpression activeCompany() {
        return company.deletedAt.isNull();
    }

    private BooleanExpression containsName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return product.name.containsIgnoreCase(name);
    }

    private BooleanExpression priceGoe(Integer minPrice) {
        if (minPrice == null) {
            return null;
        }
        return product.price.value.goe(minPrice);
    }

    private BooleanExpression priceLoe(Integer maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return product.price.value.loe(maxPrice);
    }

    private BooleanExpression stockQuantityGoe(Integer minStockQuantity) {
        if (minStockQuantity == null) {
            return null;
        }
        return product.stockQuantity.goe(minStockQuantity);
    }

    private BooleanExpression stockQuantityLoe(Integer maxStockQuantity) {
        if (maxStockQuantity == null) {
            return null;
        }
        return product.stockQuantity.loe(maxStockQuantity);
    }
}
