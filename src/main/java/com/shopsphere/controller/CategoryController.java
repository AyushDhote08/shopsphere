package com.shopsphere.controller;

import com.shopsphere.dto.category.CategoryRequestDTO;
import com.shopsphere.dto.category.CategoryResponseDTO;
import com.shopsphere.dto.common.ApiResponse;
import com.shopsphere.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category Management", description = "Endpoints for creating, updating, retrieving, and organizing categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List all active categories", description = "Retrieves a flat list of all active categories")
    public ResponseEntity<ApiResponse<List<CategoryResponseDTO>>> getAllCategories() {
        List<CategoryResponseDTO> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/hierarchy")
    @Operation(summary = "Get category hierarchy", description = "Retrieves root categories with their nested active subcategories")
    public ResponseEntity<ApiResponse<List<CategoryResponseDTO>>> getCategoryHierarchy() {
        List<CategoryResponseDTO> hierarchy = categoryService.getCategoryHierarchy();
        return ResponseEntity.ok(ApiResponse.success(hierarchy));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieves category details by numeric ID")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getCategoryById(@PathVariable Long id) {
        CategoryResponseDTO category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug", description = "Retrieves category details by URL slug")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponseDTO category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping
    @Operation(summary = "Create category", description = "Creates a new category with uniqueness validation")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> createCategory(@Valid @RequestBody CategoryRequestDTO requestDTO) {
        CategoryResponseDTO created = categoryService.createCategory(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category", description = "Updates an existing category by ID")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO requestDTO) {
        CategoryResponseDTO updated = categoryService.updateCategory(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate category", description = "Soft-deactivates category to preserve relational integrity")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
