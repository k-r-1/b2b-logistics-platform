package com.boxoffice.companyservice.product.controller;

import com.boxoffice.common.exception.GlobalExceptionHandler;
import com.boxoffice.companyservice.product.dto.response.ProductResponseDto;
import com.boxoffice.companyservice.product.dto.search.ProductSearchCondition;
import com.boxoffice.companyservice.product.service.ProductFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ProductSearchController 테스트")
class ProductSearchControllerTest {

    private final ProductFacade productFacade = mock(ProductFacade.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProductSearchController(productFacade))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("성공 - GET /api/v1/products 요청 시 전체 상품 검색 결과를 반환한다")
    void searchProductsReturnsPageResponse() throws Exception {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID userHubId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        ProductResponseDto response = createProductResponse(productId, companyId, hubId);
        Page<ProductResponseDto> responsePage = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        when(productFacade.searchProducts(any(), any(), eq("MASTER"), eq(userHubId), eq(keycloakSub)))
                .thenReturn(responsePage);

        // when & then
        mockMvc.perform(get("/api/v1/products")
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Hub-Id", userHubId.toString())
                        .header("X-User-Id", keycloakSub)
                        .param("companyId", companyId.toString())
                        .param("hubId", hubId.toString())
                        .param("name", "테스트")
                        .param("minPrice", "1000")
                        .param("maxPrice", "20000")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("SUCCESS")))
                .andExpect(jsonPath("$.data.content[0].productId", is(productId.toString())))
                .andExpect(jsonPath("$.data.content[0].companyId", is(companyId.toString())))
                .andExpect(jsonPath("$.data.content[0].hubId", is(hubId.toString())))
                .andExpect(jsonPath("$.data.content[0].name", is("테스트 상품")))
                .andExpect(jsonPath("$.data.content[0].price", is(10000)))
                .andExpect(jsonPath("$.data.content[0].stockQuantity", is(50)));

        ArgumentCaptor<ProductSearchCondition> conditionCaptor = ArgumentCaptor.forClass(ProductSearchCondition.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productFacade).searchProducts(
                conditionCaptor.capture(),
                pageableCaptor.capture(),
                eq("MASTER"),
                eq(userHubId),
                eq(keycloakSub)
        );
        verifyNoMoreInteractions(productFacade);

        ProductSearchCondition condition = conditionCaptor.getValue();
        assertThat(condition.getCompanyId()).isEqualTo(companyId);
        assertThat(condition.getHubId()).isEqualTo(hubId);
        assertThat(condition.getName()).isEqualTo("테스트");
        assertThat(condition.getMinPrice()).isEqualTo(1000);
        assertThat(condition.getMaxPrice()).isEqualTo(20000);

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    private ProductResponseDto createProductResponse(UUID productId, UUID companyId, UUID hubId) {
        ProductResponseDto response = createEmptyProductResponse();
        ReflectionTestUtils.setField(response, "productId", productId);
        ReflectionTestUtils.setField(response, "companyId", companyId);
        ReflectionTestUtils.setField(response, "hubId", hubId);
        ReflectionTestUtils.setField(response, "name", "테스트 상품");
        ReflectionTestUtils.setField(response, "price", 10000);
        ReflectionTestUtils.setField(response, "stockQuantity", 50);
        return response;
    }

    private ProductResponseDto createEmptyProductResponse() {
        try {
            Constructor<ProductResponseDto> constructor = ProductResponseDto.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ProductResponseDto 테스트 객체 생성에 실패했습니다.", e);
        }
    }
}
