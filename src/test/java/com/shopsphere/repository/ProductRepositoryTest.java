package com.shopsphere.repository;

import com.shopsphere.entity.Category;
import com.shopsphere.entity.Product;
import com.shopsphere.repository.specification.ProductSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category parentCategory;
    private Category subCategory;
    private Product product1;
    private Product product2;
    private Product outOfStockProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // 1. Root Category
        parentCategory = new Category("Electronics", "electronics", "Tech devices", null);
        parentCategory = categoryRepository.save(parentCategory);

        // 2. Sub Category
        subCategory = new Category("Audio", "audio", "Sound devices", parentCategory);
        subCategory = categoryRepository.save(subCategory);

        // 3. In-stock product in subcategory
        product1 = new Product(
                "SKU-ANC-01",
                "AcousticPro ANC Headphones",
                "acousticpro-anc-headphones",
                "Over-ear noise cancelling headphones",
                new BigDecimal("80.00"),
                new BigDecimal("199.99"),
                20,
                5,
                "https://example.com/headphones.jpg",
                subCategory
        );
        productRepository.save(product1);

        // 4. In-stock product in parent category
        product2 = new Product(
                "SKU-LAP-02",
                "ProBook Ultrabook Laptop",
                "probook-ultrabook-laptop",
                "High performance laptop",
                new BigDecimal("600.00"),
                new BigDecimal("999.00"),
                10,
                2,
                "https://example.com/laptop.jpg",
                parentCategory
        );
        productRepository.save(product2);

        // 5. Out-of-stock product
        outOfStockProduct = new Product(
                "SKU-OOS-03",
                "Vintage Record Player",
                "vintage-record-player",
                "Classic vinyl turntable",
                new BigDecimal("40.00"),
                new BigDecimal("89.99"),
                0,
                2,
                "https://example.com/player.jpg",
                subCategory
        );
        productRepository.save(outOfStockProduct);
    }

    @Test
    @DisplayName("ProductSpecification: Should search products by keyword in name, description, or SKU")
    void shouldSearchByKeyword() {
        Specification<Product> spec = ProductSpecification.filter(
                "acoustic", null, null, null, false, true
        );
        List<Product> results = productRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSku()).isEqualTo("SKU-ANC-01");
    }

    @Test
    @DisplayName("ProductSpecification: Querying parent category ID should include subcategory products")
    void shouldFindProductsUnderParentCategory() {
        // Querying with parentCategory.getId() (Electronics) should match both product1 (Audio) and product2 (Electronics)
        Specification<Product> spec = ProductSpecification.filter(
                null, parentCategory.getId(), null, null, false, true
        );
        List<Product> results = productRepository.findAll(spec);

        assertThat(results).hasSize(3); // product1, product2, and outOfStockProduct
    }

    @Test
    @DisplayName("ProductSpecification: Querying subcategory ID should return only subcategory products")
    void shouldFindProductsUnderSubCategory() {
        Specification<Product> spec = ProductSpecification.filter(
                null, subCategory.getId(), null, null, false, true
        );
        List<Product> results = productRepository.findAll(spec);

        assertThat(results).hasSize(2); // product1 and outOfStockProduct
        assertThat(results).extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-ANC-01", "SKU-OOS-03");
    }

    @Test
    @DisplayName("ProductSpecification: Should filter products within price boundaries")
    void shouldFilterByPriceRange() {
        Specification<Product> spec = ProductSpecification.filter(
                null, null, new BigDecimal("100.00"), new BigDecimal("300.00"), false, true
        );
        List<Product> results = productRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSku()).isEqualTo("SKU-ANC-01"); // $199.99
    }

    @Test
    @DisplayName("ProductSpecification: Should filter in-stock products only")
    void shouldFilterInStockOnly() {
        Specification<Product> spec = ProductSpecification.filter(
                null, null, null, null, true, true
        );
        List<Product> results = productRepository.findAll(spec);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Product::getSku)
                .doesNotContain("SKU-OOS-03");
    }

    @Test
    @DisplayName("ProductSpecification: Should combine query, category, price, and stock filters")
    void shouldCombineAllFilters() {
        Specification<Product> spec = ProductSpecification.filter(
                "noise", parentCategory.getId(), new BigDecimal("150.00"), new BigDecimal("250.00"), true, true
        );
        Page<Product> page = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("AcousticPro ANC Headphones");
    }
}
