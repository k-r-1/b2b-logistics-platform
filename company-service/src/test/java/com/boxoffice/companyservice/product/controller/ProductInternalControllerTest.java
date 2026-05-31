package com.boxoffice.companyservice.product.controller;

import com.boxoffice.common.exception.GlobalExceptionHandler;
import com.boxoffice.companyservice.product.dto.response.HubStockCountResponseDto;
import com.boxoffice.companyservice.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ProductInternalController 테스트")
class ProductInternalControllerTest {

    private final ProductService productService = mock(ProductService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProductInternalController(productService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("성공 - 여러 허브의 재고 합계를 반환한다")
    void getHubStockCountsReturnsStockCounts() throws Exception {
        // given
        UUID firstHubId = UUID.randomUUID();
        UUID secondHubId = UUID.randomUUID();
        List<UUID> hubIds = List.of(firstHubId, secondHubId);

        when(productService.getHubStockCounts(hubIds)).thenReturn(List.of(
                new HubStockCountResponseDto(firstHubId, 500L),
                new HubStockCountResponseDto(secondHubId, 0L)
        ));

        // when & then
        mockMvc.perform(post("/internal/v1/products/hubs/stock-counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hubIds": ["%s", "%s"]
                                }
                                """.formatted(firstHubId, secondHubId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("SUCCESS")))
                .andExpect(jsonPath("$.data[0].hubId", is(firstHubId.toString())))
                .andExpect(jsonPath("$.data[0].stockCount", is(500)))
                .andExpect(jsonPath("$.data[1].hubId", is(secondHubId.toString())))
                .andExpect(jsonPath("$.data[1].stockCount", is(0)));

        verify(productService).getHubStockCounts(hubIds);
        verifyNoMoreInteractions(productService);
    }

    @Test
    @DisplayName("실패 - hubIds가 비어 있으면 validation error를 반환한다")
    void getHubStockCountsWithEmptyHubIdsReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/internal/v1/products/hubs/stock-counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hubIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("성공 - 주문 상품 재고 확인 요청을 처리한다")
    void checkStocksReturnsSuccess() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(post("/internal/v1/products/stocks/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  { "productId": "%s", "quantity": 2 }
                                ]
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("SUCCESS")));
    }

    @Test
    @DisplayName("성공 - 주문 상품 재고 차감 요청을 처리한다")
    void deductStocksReturnsSuccess() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        mockMvc.perform(post("/internal/v1/products/stocks/deduct")
                        .param("orderId", orderId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": "%s",
                                  "receiverId": "%s",
                                  "products": [
                                    { "productId": "%s", "quantity": 2 }
                                  ]
                                }
                                """.formatted(supplierId, receiverId, productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("SUCCESS")));
    }

    @Test
    @DisplayName("실패 - 재고 차감 요청의 orderId가 없으면 validation error를 반환한다")
    void deductStocksWithoutOrderIdReturnsBadRequest() throws Exception {
        UUID supplierId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        mockMvc.perform(post("/internal/v1/products/stocks/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": "%s",
                                  "receiverId": "%s",
                                  "products": [
                                    { "productId": "%s", "quantity": 2 }
                                  ]
                                }
                """.formatted(supplierId, receiverId, productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("성공 - 주문 상품 재고 복원 요청을 처리한다")
    void restoreStocksReturnsSuccess() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        mockMvc.perform(post("/internal/v1/products/stocks/restore")
                        .param("orderId", orderId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  { "productId": "%s", "quantity": 2 }
                                ]
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("SUCCESS")));
    }
}
