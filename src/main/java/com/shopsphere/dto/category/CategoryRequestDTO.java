package com.shopsphere.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for creating or updating a product category")
public class CategoryRequestDTO {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    @Schema(description = "Display name of the category", example = "Smart Home & IoT")
    private String name;

    @Size(max = 120, message = "Slug cannot exceed 120 characters")
    @Schema(description = "URL-friendly slug (auto-generated from name if omitted)", example = "smart-home-iot")
    private String slug;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Schema(description = "Detailed category description", example = "Automated appliances and smart home sensors")
    private String description;

    @Schema(description = "Optional parent category ID for subcategory hierarchy", example = "1")
    private Long parentId;

    @Schema(description = "Whether the category is active and visible", example = "true")
    private Boolean isActive = true;

    public CategoryRequestDTO() {
    }

    public CategoryRequestDTO(String name, String slug, String description, Long parentId, Boolean isActive) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.parentId = parentId;
        this.isActive = isActive != null ? isActive : true;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
