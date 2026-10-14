package com.shopsphere.service;

import com.shopsphere.dto.category.CategoryRequestDTO;
import com.shopsphere.dto.category.CategoryResponseDTO;
import com.shopsphere.entity.Category;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category mockCategory;

    @BeforeEach
    void setUp() {
        mockCategory = new Category("Wearables", "wearables", "Wearable gadgets", null);
        mockCategory.setId(10L);
    }

    @Test
    @DisplayName("getCategoryById: Should return category when found")
    void shouldReturnCategoryById() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(mockCategory));

        CategoryResponseDTO response = categoryService.getCategoryById(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Wearables");
        verify(categoryRepository, times(1)).findById(10L);
    }

    @Test
    @DisplayName("getCategoryById: Should throw ResourceNotFoundException when ID does not exist")
    void shouldThrowExceptionWhenCategoryNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found with id: '99'");
    }

    @Test
    @DisplayName("createCategory: Should successfully create and save category")
    void shouldCreateCategorySuccessfully() {
        CategoryRequestDTO request = new CategoryRequestDTO("Smart Home", null, "Smart devices", null, true);

        when(categoryRepository.existsByNameIgnoreCase("Smart Home")).thenReturn(false);
        when(categoryRepository.existsBySlug("smart-home")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(20L);
            return c;
        });

        CategoryResponseDTO created = categoryService.createCategory(request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(20L);
        assertThat(created.getName()).isEqualTo("Smart Home");
        assertThat(created.getSlug()).isEqualTo("smart-home");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory: Should throw DuplicateResourceException when name already exists")
    void shouldThrowExceptionOnDuplicateName() {
        CategoryRequestDTO request = new CategoryRequestDTO("Wearables", null, "Desc", null, true);

        when(categoryRepository.existsByNameIgnoreCase("Wearables")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("deleteCategory: Should soft-deactivate category")
    void shouldSoftDeleteCategory() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(mockCategory));

        categoryService.deleteCategory(10L);

        assertThat(mockCategory.isActive()).isFalse();
        verify(categoryRepository).save(mockCategory);
    }
}
