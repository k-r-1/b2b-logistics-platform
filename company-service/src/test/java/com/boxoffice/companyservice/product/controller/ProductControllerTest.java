package com.boxoffice.companyservice.product.controller;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.exception.GlobalExceptionHandler;
import com.boxoffice.companyservice.product.dto.request.ProductCreateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import com.boxoffice.companyservice.product.service.ProductFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Constructor;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ProductController 테스트")
class ProductControllerTest {

    private final ProductFacade productFacade = mock(ProductFacade.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProductController(productFacade))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("성공 - POST /api/v1/companies/{companyId}/products 요청 시 상품을 생성하고 201 Created를 반환한다")
    void createProductReturnsCreated() throws Exception {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID userHubId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        ProductCreateResponseDto response = createResponse(productId, companyId);

        when(productFacade.createProduct(any(), any(), any(), any(), any())).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/companies/{companyId}/products", companyId)
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Hub-Id", userHubId.toString())
                        .header("X-User-Id", keycloakSub)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(201)))
                .andExpect(jsonPath("$.message", is("SUCCESS")))
                .andExpect(jsonPath("$.data.productId", is(productId.toString())))
                .andExpect(jsonPath("$.data.companyId", is(companyId.toString())))
                .andExpect(jsonPath("$.data.name", is("테스트 상품")))
                .andExpect(jsonPath("$.data.price", is(10000)))
                .andExpect(jsonPath("$.data.stockQuantity", is(50)));

        ArgumentCaptor<ProductCreateRequestDto> requestCaptor = ArgumentCaptor.forClass(ProductCreateRequestDto.class);
        verify(productFacade).createProduct(eq(companyId), requestCaptor.capture(), eq("MASTER"), eq(userHubId), eq(keycloakSub));
        verifyNoMoreInteractions(productFacade);

        ProductCreateRequestDto request = requestCaptor.getValue();
        assertThat(request.getName()).isEqualTo("테스트 상품");
        assertThat(request.getPrice()).isEqualTo(10000);
        assertThat(request.getStockQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("실패 - 상품명 누락 요청은 400 검증 실패를 반환한다")
    void createProductWithoutNameReturnsValidationError() throws Exception {
        // given
        UUID companyId = UUID.randomUUID();
        String requestBody = """
                {
                  "price": 10000,
                  "stockQuantity": 50
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/companies/{companyId}/products", companyId)
                        .header("X-User-Role", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors", hasItem("name: 상품명은 필수입니다.")));

        verifyNoInteractions(productFacade);
    }

    @Test
    @DisplayName("실패 - 음수 가격 요청은 400 검증 실패를 반환한다")
    void createProductWithNegativePriceReturnsValidationError() throws Exception {
        // given
        UUID companyId = UUID.randomUUID();
        String requestBody = """
                {
                  "name": "테스트 상품",
                  "price": -1,
                  "stockQuantity": 50
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/companies/{companyId}/products", companyId)
                        .header("X-User-Role", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors", hasItem("price: 상품 가격은 0 이상이어야 합니다.")));

        verifyNoInteractions(productFacade);
    }

    @Test
    @DisplayName("실패 - 음수 재고 요청은 400 검증 실패를 반환한다")
    void createProductWithNegativeStockQuantityReturnsValidationError() throws Exception {
        // given
        UUID companyId = UUID.randomUUID();
        String requestBody = """
                {
                  "name": "테스트 상품",
                  "price": 10000,
                  "stockQuantity": -1
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/companies/{companyId}/products", companyId)
                        .header("X-User-Role", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors", hasItem("stockQuantity: 상품 재고 수량은 0 이상이어야 합니다.")));

        verifyNoInteractions(productFacade);
    }

    @Test
    @DisplayName("실패 - role 헤더가 없으면 401 인증 실패를 반환한다")
    void createProductWithoutRoleReturnsUnauthorized() throws Exception {
        // given
        UUID companyId = UUID.randomUUID();
        doThrow(new BaseException(CommonErrorCode.UNAUTHORIZED))
                .when(productFacade).createProduct(eq(companyId), any(), isNull(), isNull(), isNull());

        // when & then
        mockMvc.perform(post("/api/v1/companies/{companyId}/products", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is(CommonErrorCode.UNAUTHORIZED.getMessage())));

        verify(productFacade).createProduct(eq(companyId), any(), isNull(), isNull(), isNull());
        verifyNoMoreInteractions(productFacade);
    }

    @Test
    @DisplayName("실패 - 상품 생성 권한이 없으면 403 인가 실패를 반환한다")
    void createProductWithForbiddenRoleReturnsForbidden() throws Exception {
        // given
        UUID companyId = UUID.randomUUID();
        String keycloakSub = UUID.randomUUID().toString();
        doThrow(new BaseException(CommonErrorCode.FORBIDDEN))
                .when(productFacade).createProduct(eq(companyId), any(), eq("DELIVERY_MANAGER"), isNull(), eq(keycloakSub));

        // when & then
        mockMvc.perform(post("/api/v1/companies/{companyId}/products", companyId)
                        .header("X-User-Role", "DELIVERY_MANAGER")
                        .header("X-User-Id", keycloakSub)
                        .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", is(CommonErrorCode.FORBIDDEN.getMessage())));

        verify(productFacade).createProduct(eq(companyId), any(), eq("DELIVERY_MANAGER"), isNull(), eq(keycloakSub));
        verifyNoMoreInteractions(productFacade);
    }

    private ProductCreateResponseDto createResponse(UUID productId, UUID companyId) {
        ProductCreateResponseDto response = createEmptyResponse();
        ReflectionTestUtils.setField(response, "productId", productId);
        ReflectionTestUtils.setField(response, "companyId", companyId);
        ReflectionTestUtils.setField(response, "name", "테스트 상품");
        ReflectionTestUtils.setField(response, "price", 10000);
        ReflectionTestUtils.setField(response, "stockQuantity", 50);
        return response;
    }

    private ProductCreateResponseDto createEmptyResponse() {
        try {
            Constructor<ProductCreateResponseDto> constructor = ProductCreateResponseDto.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ProductCreateResponseDto 테스트 객체 생성에 실패했습니다.", e);
        }
    }

    private String createRequestBody() {
        return """
                {
                  "name": "테스트 상품",
                  "price": 10000,
                  "stockQuantity": 50
                }
                """;
    }
}
