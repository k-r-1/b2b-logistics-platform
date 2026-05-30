package com.boxoffice.companyservice.product.service;

import com.boxoffice.companyservice.company.entity.Company;
import com.boxoffice.companyservice.company.entity.CompanyType;
import com.boxoffice.companyservice.company.repository.CompanyRepository;
import com.boxoffice.companyservice.product.dto.request.ProductCreateRequestDto;
import com.boxoffice.companyservice.product.dto.response.ProductCreateResponseDto;
import com.boxoffice.companyservice.product.entity.Product;
import com.boxoffice.companyservice.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    private ProductCreateRequestDto createRequest(String name, Integer price, Integer stockQuantity) {
        ProductCreateRequestDto request = new ProductCreateRequestDto();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "price", price);
        ReflectionTestUtils.setField(request, "stockQuantity", stockQuantity);
        return request;
    }
}
