package com.shopsphere.repository;

import com.shopsphere.entity.Category;
import com.shopsphere.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        testCategory = new Category("Audio", "audio", "Audio items", null);
        testCategory = categoryRepository.save(testCategory);

        product1 = new Product(
                "ELEC-AUD-001",
                "AcousticPro ANC Wireless Headphones",
                "acousticpro-anc-wireless-headphones",
                "Over-ear active noise cancelling",
                new BigDecimal("85.00"),
                new BigDecimal("199.99"),
                45,
                5,
                "https://example.com/headphones.jpg",
                testCategory
        );
        product1 = productRepository.save(product1);

        product2 = new Product(
                "ELEC-AUD-002",
                "SoundPulse Bluetooth Speaker",
                "soundpulse-bluetooth-speaker",
                "Waterproof portable speaker",
                new BigDecimal("28.50"),
                new BigDecimal("69.99"),
                3, // Low stock: 3 <= 5
                5,
                "https://example.com/speaker.jpg",
                testCategory
        );
        product2 = productRepository.save(product2);
    }

    @Test
    @DisplayName("findBySku: Should return product with matching SKU")
    void shouldFindProductBySku() {
        Optional<Product> found = productRepository.findBySku("ELEC-AUD-001");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("AcousticPro ANC Wireless Headphones");
        assertThat(found.get().getSellingPrice()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(found.get().getCategory().getName()).isEqualTo("Audio");
    }

    @Test
    @DisplayName("findBySlug: Should return product with matching slug")
    void shouldFindProductBySlug() {
        Optional<Product> found = productRepository.findBySlug("soundpulse-bluetooth-speaker");

        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("ELEC-AUD-002");
    }

    @Test
    @DisplayName("existsBySku: Should correctly report whether SKU is taken")
    void shouldVerifySkuExistence() {
        assertThat(productRepository.existsBySku("ELEC-AUD-001")).isTrue();
        assertThat(productRepository.existsBySku("NON-EXISTENT-SKU-999")).isFalse();
    }

    @Test
    @DisplayName("findLowStockProducts: Should return items at or below low stock threshold")
    void shouldFindLowStockProducts() {
        List<Product> lowStockItems = productRepository.findLowStockProducts();

        assertThat(lowStockItems).hasSize(1);
        assertThat(lowStockItems.get(0).getSku()).isEqualTo("ELEC-AUD-002");
    }

    @Test
    @DisplayName("save: Should persist a new custom product with BigDecimal prices")
    void shouldSaveNewProduct() {
        Product customProduct = new Product(
                "TEST-DEV-999",
                "Developer Keyboard",
                "developer-keyboard",
                "Mechanical keyboard",
                new BigDecimal("65.00"),
                new BigDecimal("129.99"),
                20,
                5,
                "https://example.com/keyboard.jpg",
                testCategory
        );

        Product saved = productRepository.save(customProduct);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSellingPrice()).isEqualByComparingTo(new BigDecimal("129.99"));
        assertThat(productRepository.existsBySku("TEST-DEV-999")).isTrue();
    }
}
