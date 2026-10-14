package com.shopsphere.service;

import com.shopsphere.dto.category.CategoryRequestDTO;
import com.shopsphere.dto.category.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {

    List<CategoryResponseDTO> getAllActiveCategories();

    List<CategoryResponseDTO> getCategoryHierarchy();

    CategoryResponseDTO getCategoryById(Long id);

    CategoryResponseDTO getCategoryBySlug(String slug);

    CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO);

    CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO requestDTO);

    void deleteCategory(Long id);
}
