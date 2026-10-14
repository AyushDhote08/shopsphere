package com.shopsphere.repository;

import com.shopsphere.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category audio;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();

        electronics = new Category("Electronics", "electronics", "Consumer Electronics", null);
        electronics = categoryRepository.save(electronics);

        audio = new Category("Audio & Sound", "audio-sound", "Headphones and Speakers", electronics);
        audio = categoryRepository.save(audio);
    }

    @Test
    @DisplayName("findBySlug: Should return category when slug exists")
    void shouldFindCategoryBySlug() {
        Optional<Category> found = categoryRepository.findBySlug("audio-sound");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Audio & Sound");
        assertThat(found.get().getParent()).isNotNull();
        assertThat(found.get().getParent().getSlug()).isEqualTo("electronics");
    }

    @Test
    @DisplayName("findByNameIgnoreCase: Should find category irrespective of letter case")
    void shouldFindByNameIgnoreCase() {
        Optional<Category> found = categoryRepository.findByNameIgnoreCase("ELECTRONICS");

        assertThat(found).isPresent();
        assertThat(found.get().getSlug()).isEqualTo("electronics");
    }

    @Test
    @DisplayName("existsBySlug: Should return true if slug is present, false otherwise")
    void shouldCheckSlugExistence() {
        assertThat(categoryRepository.existsBySlug("electronics")).isTrue();
        assertThat(categoryRepository.existsBySlug("non-existent-slug-xyz")).isFalse();
    }

    @Test
    @DisplayName("findByParentIsNullAndIsActiveTrue: Should only return root categories")
    void shouldFindRootCategoriesOnly() {
        List<Category> rootCategories = categoryRepository.findByParentIsNullAndIsActiveTrue();

        assertThat(rootCategories).hasSize(1);
        assertThat(rootCategories.get(0).getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("save: Should persist a new custom category")
    void shouldSaveNewCategory() {
        Category newCat = new Category("Wearables", "wearables", "Smartwatches", null);
        Category saved = categoryRepository.save(newCat);

        assertThat(saved.getId()).isNotNull();
        assertThat(categoryRepository.existsBySlug("wearables")).isTrue();
    }
}
