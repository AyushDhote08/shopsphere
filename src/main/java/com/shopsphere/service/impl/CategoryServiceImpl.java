package com.shopsphere.service.impl;

import com.shopsphere.dto.category.CategoryRequestDTO;
import com.shopsphere.dto.category.CategoryResponseDTO;
import com.shopsphere.entity.Category;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getCategoryHierarchy() {
        return categoryRepository.findByParentIsNullAndIsActiveTrue().stream()
                .map(this::mapToDTOWithChildren)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return mapToDTOWithChildren(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return mapToDTOWithChildren(category);
    }

    @Override
    public CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO) {
        if (categoryRepository.existsByNameIgnoreCase(requestDTO.getName())) {
            throw new DuplicateResourceException("Category", "name", requestDTO.getName());
        }

        String slug = generateSlug(requestDTO.getSlug(), requestDTO.getName());
        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Category", "slug", slug);
        }

        Category parent = null;
        if (requestDTO.getParentId() != null) {
            parent = categoryRepository.findById(requestDTO.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", requestDTO.getParentId()));
        }

        Category category = new Category();
        category.setName(requestDTO.getName().trim());
        category.setSlug(slug);
        category.setDescription(requestDTO.getDescription());
        category.setParent(parent);
        category.setActive(requestDTO.getIsActive() != null ? requestDTO.getIsActive() : true);

        Category savedCategory = categoryRepository.save(category);
        return mapToDTO(savedCategory);
    }

    @Override
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO requestDTO) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Check if name is taken by another category
        categoryRepository.findByNameIgnoreCase(requestDTO.getName().trim())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new DuplicateResourceException("Category", "name", requestDTO.getName());
                    }
                });

        String slug = generateSlug(requestDTO.getSlug(), requestDTO.getName());
        categoryRepository.findBySlug(slug)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new DuplicateResourceException("Category", "slug", slug);
                    }
                });

        Category parent = null;
        if (requestDTO.getParentId() != null) {
            if (requestDTO.getParentId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            parent = categoryRepository.findById(requestDTO.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", requestDTO.getParentId()));
        }

        category.setName(requestDTO.getName().trim());
        category.setSlug(slug);
        category.setDescription(requestDTO.getDescription());
        category.setParent(parent);
        if (requestDTO.getIsActive() != null) {
            category.setActive(requestDTO.getIsActive());
        }

        Category updatedCategory = categoryRepository.save(category);
        return mapToDTO(updatedCategory);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Soft deactivation to preserve relations and avoid orphan records
        category.setActive(false);
        categoryRepository.save(category);
    }

    private String generateSlug(String customSlug, String name) {
        if (customSlug != null && !customSlug.trim().isEmpty()) {
            return customSlug.trim().toLowerCase()
                    .replaceAll("[^a-z0-9-]+", "-")
                    .replaceAll("^-|-$", "");
        }
        return name.trim().toLowerCase()
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("^-|-$", "");
    }

    private CategoryResponseDTO mapToDTO(Category category) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());
        dto.setDescription(category.getDescription());
        dto.setActive(category.isActive());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());

        if (category.getParent() != null) {
            dto.setParentId(category.getParent().getId());
            dto.setParentName(category.getParent().getName());
        }
        return dto;
    }

    private CategoryResponseDTO mapToDTOWithChildren(Category category) {
        CategoryResponseDTO dto = mapToDTO(category);
        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            dto.setSubcategories(category.getSubcategories().stream()
                    .filter(Category::isActive)
                    .map(this::mapToDTOWithChildren)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
