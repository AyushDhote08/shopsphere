package com.shopsphere.service;

import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.product.ProductRequestDTO;
import com.shopsphere.dto.product.ProductResponseDTO;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Product;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category mockCategory;
    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockCategory = new Category("Audio", "audio", "Audio items", null);
        mockCategory.setId(1L);

        mockProduct = new Product(
                "TEST-SKU-1",
                "Wireless Earbuds",
                "wireless-earbuds",
                "Noise isolation",
                new BigDecimal("30.00"),
                new BigDecimal("79.99"),
                25,
                5,
                "https://example.com/earbuds.jpg",
                mockCategory
        );
        mockProduct.setId(100L);
    }

    @Test
    @DisplayName("getProductById: Should return product response DTO")
    void shouldReturnProductById() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(mockProduct));

        ProductResponseDTO result = productService.getProductById(100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getSku()).isEqualTo("TEST-SKU-1");
        assertThat(result.getSellingPrice()).isEqualByComparingTo(new BigDecimal("79.99"));
        assertThat(result.isInStock()).isTrue();
        assertThat(result.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("getProductById: Should throw ResourceNotFoundException when product does not exist")
    void shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: '999'");
    }

    @Test
    @DisplayName("createProduct: Should successfully persist product and return DTO")
    void shouldCreateProductSuccessfully() {
        ProductRequestDTO request = new ProductRequestDTO(
                "NEW-SKU-2",
                "Pro Headset",
                null,
                "Studio headset",
                new BigDecimal("50.00"),
                new BigDecimal("120.00"),
                40,
                5,
                "https://example.com/headset.jpg",
                1L,
                true
        );

        when(productRepository.existsBySku("NEW-SKU-2")).thenReturn(false);
        when(productRepository.existsBySlug("pro-headset")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(200L);
            return p;
        });

        ProductResponseDTO created = productService.createProduct(request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(200L);
        assertThat(created.getSku()).isEqualTo("NEW-SKU-2");
        assertThat(created.getSlug()).isEqualTo("pro-headset");
        assertThat(created.getCategoryName()).isEqualTo("Audio");
    }

    @Test
    @DisplayName("createProduct: Should throw DuplicateResourceException on existing SKU")
    void shouldThrowOnDuplicateSku() {
        ProductRequestDTO request = new ProductRequestDTO(
                "TEST-SKU-1", "Duplicate Item", null, null,
                new BigDecimal("10.00"), new BigDecimal("20.00"), 10, 2, null, 1L, true
        );

        when(productRepository.existsBySku("TEST-SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists with sku: 'TEST-SKU-1'");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("getProducts: Should query repository specification and return PageResponse")
    void shouldReturnPaginatedProducts() {
        Page<Product> page = new PageImpl<>(List.of(mockProduct));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponseDTO> response = productService.getProducts(
                "earbuds", 1L, null, null, false, 0, 10, "createdAt", "DESC"
        );

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Wireless Earbuds");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getProducts: Should throw BadRequestException when minPrice is negative")
    void shouldThrowWhenMinPriceIsNegative() {
        assertThatThrownBy(() -> productService.getProducts(
                null, null, new BigDecimal("-1.00"), null, false, 0, 10, "createdAt", "DESC"
        ))
                .isInstanceOf(com.shopsphere.exception.BadRequestException.class)
                .hasMessageContaining("Minimum price cannot be negative");
    }

    @Test
    @DisplayName("getProducts: Should throw BadRequestException when maxPrice is negative")
    void shouldThrowWhenMaxPriceIsNegative() {
        assertThatThrownBy(() -> productService.getProducts(
                null, null, null, new BigDecimal("-10.00"), false, 0, 10, "createdAt", "DESC"
        ))
                .isInstanceOf(com.shopsphere.exception.BadRequestException.class)
                .hasMessageContaining("Maximum price cannot be negative");
    }

    @Test
    @DisplayName("getProducts: Should throw BadRequestException when minPrice exceeds maxPrice")
    void shouldThrowWhenMinPriceExceedsMaxPrice() {
        assertThatThrownBy(() -> productService.getProducts(
                null, null, new BigDecimal("200.00"), new BigDecimal("50.00"), false, 0, 10, "createdAt", "DESC"
        ))
                .isInstanceOf(com.shopsphere.exception.BadRequestException.class)
                .hasMessageContaining("Minimum price cannot be greater than maximum price");
    }
}
