package com.boxoffice.companyservice.product.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.boxoffice.companyservice.company.repository.CompanyRepository;
import com.boxoffice.companyservice.product.domain.PriceVO;
import com.boxoffice.companyservice.product.dto.request.ProductCreateRequestDto;
import com.boxoffice.companyservice.product.dto.request.ProductUpdateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import com.boxoffice.companyservice.product.dto.response.HubStockCountResponseDto;
import com.boxoffice.companyservice.product.dto.response.ProductResponseDto;
import com.boxoffice.companyservice.product.dto.search.ProductSearchCondition;
import com.boxoffice.companyservice.product.entity.Product;
import com.boxoffice.companyservice.product.exception.ProductErrorCode;
import com.boxoffice.companyservice.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 테스트")
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("성공 - 요청을 상품 엔티티로 변환해 저장하고 생성 응답으로 반환한다")
    void createProductConvertsRequestToEntitySavesAndReturnsResponse() {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Company company = createCompany(companyId);
        ProductCreateRequestDto request = createRequest(" 테스트 상품 ", 10000, 50);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", productId);
            return product;
        });

        // when
        ProductCreateResponseDto response = productService.createProduct(companyId, request);

        // then
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(companyRepository).findById(companyId);
        verify(productRepository).save(productCaptor.capture());
        verifyNoMoreInteractions(companyRepository, productRepository);

        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getName()).isEqualTo("테스트 상품");
        assertThat(savedProduct.getPrice().getValue()).isEqualTo(10000);
        assertThat(savedProduct.getStockQuantity()).isEqualTo(50);
        assertThat(savedProduct.getCompany()).isSameAs(company);
        assertThat(savedProduct.isDeleted()).isFalse();
        assertThat(savedProduct.getDeletedAt()).isNull();

        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getCompanyId()).isEqualTo(companyId);
        assertThat(response.getName()).isEqualTo("테스트 상품");
        assertThat(response.getPrice()).isEqualTo(10000);
        assertThat(response.getStockQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("성공 - 수정 요청의 null 필드는 유지하고 전달된 필드만 변경한다")
    void updateProductChangesOnlyRequestedFields() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Company company = createCompany(companyId);
        Product product = createProduct(productId, company);
        ProductUpdateRequestDto request = createUpdateRequest(" 수정 상품 ", 15000, null);

        when(productRepository.findProduct(companyId, productId)).thenReturn(Optional.of(product));

        // when
        productService.updateProduct(companyId, productId, request);

        // then
        verify(productRepository).findProduct(companyId, productId);
        verifyNoMoreInteractions(productRepository);

        assertThat(product.getName()).isEqualTo("수정 상품");
        assertThat(product.getPrice().getValue()).isEqualTo(15000);
        assertThat(product.getStockQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("실패 - 조회 조건에 맞는 상품이 없으면 수정할 수 없다")
    void updateProductWhenProductNotFoundThrowsNotFound() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductUpdateRequestDto request = createUpdateRequest("수정 상품", 15000, 30);

        when(productRepository.findProduct(companyId, productId)).thenReturn(Optional.empty());

        // when
        Throwable throwable = catchThrowable(() -> productService.updateProduct(companyId, productId, request));

        // then
        assertProductNotFound(throwable);
        verify(productRepository).findProduct(companyId, productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    @DisplayName("성공 - 상품을 soft delete 처리한다")
    void deleteProductSoftDeletesProduct() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID deletedBy = UUID.randomUUID();
        Product product = createProduct(productId, createCompany(companyId));

        when(productRepository.findProduct(companyId, productId)).thenReturn(Optional.of(product));

        // when
        productService.deleteProduct(companyId, productId, deletedBy);

        // then
        verify(productRepository).findProduct(companyId, productId);
        verifyNoMoreInteractions(productRepository);

        assertThat(product.isDeleted()).isTrue();
        assertThat(product.getDeletedAt()).isNotNull();
        assertThat(product.getDeletedBy()).isEqualTo(deletedBy);
    }

    @Test
    @DisplayName("실패 - 조회 조건에 맞는 상품이 없으면 삭제할 수 없다")
    void deleteProductWhenProductNotFoundThrowsNotFound() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID deletedBy = UUID.randomUUID();

        when(productRepository.findProduct(companyId, productId)).thenReturn(Optional.empty());

        // when
        Throwable throwable = catchThrowable(() -> productService.deleteProduct(companyId, productId, deletedBy));

        // then
        assertProductNotFound(throwable);
        verify(productRepository).findProduct(companyId, productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    @DisplayName("성공 - 상품 상세를 조회 응답으로 변환해 반환한다")
    void getProductReturnsProductResponse() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Company company = createCompany(companyId, hubId);
        Product product = createProduct(productId, company);

        when(productRepository.findProduct(companyId, productId)).thenReturn(Optional.of(product));

        // when
        ProductResponseDto response = productService.getProduct(companyId, productId);

        // then
        verify(productRepository).findProduct(companyId, productId);
        verifyNoMoreInteractions(productRepository);

        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getCompanyId()).isEqualTo(companyId);
        assertThat(response.getHubId()).isEqualTo(hubId);
        assertThat(response.getName()).isEqualTo("테스트 상품");
        assertThat(response.getPrice()).isEqualTo(10000);
        assertThat(response.getStockQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("성공 - 상품 검색 결과를 조회 응답 페이지로 변환해 반환한다")
    void searchProductsReturnsProductResponsePage() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Company company = createCompany(companyId, hubId);
        Product product = createProduct(productId, company);
        ProductSearchCondition condition = new ProductSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.searchProducts(companyId, condition, pageable)).thenReturn(productPage);

        // when
        Page<ProductResponseDto> responsePage = productService.searchProducts(companyId, condition, pageable);

        // then
        verify(productRepository).searchProducts(companyId, condition, pageable);
        verifyNoMoreInteractions(productRepository);

        assertThat(responsePage.getContent()).hasSize(1);
        ProductResponseDto response = responsePage.getContent().get(0);
        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getCompanyId()).isEqualTo(companyId);
        assertThat(response.getHubId()).isEqualTo(hubId);
        assertThat(response.getName()).isEqualTo("테스트 상품");
        assertThat(response.getPrice()).isEqualTo(10000);
        assertThat(response.getStockQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("성공 - 전체 상품 검색 결과를 조회 응답 페이지로 변환해 반환한다")
    void searchAllProductsReturnsProductResponsePage() {
        // given
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Company company = createCompany(companyId, hubId);
        Product product = createProduct(productId, company);
        ProductSearchCondition condition = new ProductSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.searchProducts(condition, pageable)).thenReturn(productPage);

        // when
        Page<ProductResponseDto> responsePage = productService.searchProducts(condition, pageable);

        // then
        verify(productRepository).searchProducts(condition, pageable);
        verifyNoMoreInteractions(productRepository);

        assertThat(responsePage.getContent()).hasSize(1);
        ProductResponseDto response = responsePage.getContent().get(0);
        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getCompanyId()).isEqualTo(companyId);
        assertThat(response.getHubId()).isEqualTo(hubId);
        assertThat(response.getName()).isEqualTo("테스트 상품");
        assertThat(response.getPrice()).isEqualTo(10000);
        assertThat(response.getStockQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("성공 - 허브별 재고 합계가 없는 허브는 0으로 채워 반환한다")
    void getHubStockCountsFillsZeroForEmptyHub() {
        // given
        UUID firstHubId = UUID.randomUUID();
        UUID secondHubId = UUID.randomUUID();
        List<UUID> hubIds = List.of(firstHubId, secondHubId);

        when(productRepository.sumStockQuantityByHubIds(hubIds)).thenReturn(Map.of(firstHubId, 500L));

        // when
        List<HubStockCountResponseDto> response = productService.getHubStockCounts(hubIds);

        // then
        verify(productRepository).sumStockQuantityByHubIds(hubIds);
        verifyNoMoreInteractions(productRepository);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getHubId()).isEqualTo(firstHubId);
        assertThat(response.get(0).getStockCount()).isEqualTo(500L);
        assertThat(response.get(1).getHubId()).isEqualTo(secondHubId);
        assertThat(response.get(1).getStockCount()).isZero();
    }

    private Company createCompany(UUID companyId) {
        Company company = Company.create(
                "테스트 업체",
                CompanyType.SUPPLIER,
                UUID.randomUUID(),
                null
        );
        ReflectionTestUtils.setField(company, "id", companyId);
        return company;
    }

    private Company createCompany(UUID companyId, UUID hubId) {
        Company company = Company.create(
                "테스트 업체",
                CompanyType.SUPPLIER,
                hubId,
                null
        );
        ReflectionTestUtils.setField(company, "id", companyId);
        return company;
    }

    private Product createProduct(UUID productId, Company company) {
        Product product = Product.create(
                "테스트 상품",
                PriceVO.create(10000),
                50,
                company
        );
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }

    private ProductCreateRequestDto createRequest(String name, Integer price, Integer stockQuantity) {
        ProductCreateRequestDto request = new ProductCreateRequestDto();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "price", price);
        ReflectionTestUtils.setField(request, "stockQuantity", stockQuantity);
        return request;
    }

    private ProductUpdateRequestDto createUpdateRequest(String name, Integer price, Integer stockQuantity) {
        ProductUpdateRequestDto request = new ProductUpdateRequestDto();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "price", price);
        ReflectionTestUtils.setField(request, "stockQuantity", stockQuantity);
        return request;
    }

    private void assertProductNotFound(Throwable throwable) {
        assertThat(throwable)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND));
    }
}
