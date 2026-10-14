package com.shopsphere.dto.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Category response data")
public class CategoryResponseDTO {

    @Schema(description = "Category unique identifier", example = "1")
    private Long id;

    @Schema(description = "Category name", example = "Audio & Sound")
    private String name;

    @Schema(description = "URL slug", example = "audio-sound")
    private String slug;

    @Schema(description = "Category description")
    private String description;

    @Schema(description = "Parent category ID if applicable", example = "1")
    private Long parentId;

    @Schema(description = "Parent category name if applicable", example = "Electronics")
    private String parentName;

    @Schema(description = "Active status", example = "true")
    private boolean isActive;

    @Schema(description = "Subcategories list")
    private List<CategoryResponseDTO> subcategories;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public CategoryResponseDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public List<CategoryResponseDTO> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<CategoryResponseDTO> subcategories) {
        this.subcategories = subcategories;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
