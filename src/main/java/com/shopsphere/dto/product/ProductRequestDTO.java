package com.shopsphere.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Payload for creating or updating a product")
public class ProductRequestDTO {

    @NotBlank(message = "SKU is required")
    @Size(min = 3, max = 64, message = "SKU must be between 3 and 64 characters")
    @Pattern(regexp = "^[A-Za-z0-9-_]+$", message = "SKU must contain only alphanumeric characters, dashes, or underscores")
    @Schema(description = "Unique Stock Keeping Unit", example = "ELEC-AUD-005")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    @Schema(description = "Product display title", example = "Wireless Studio Earbuds Pro")
    private String name;

    @Size(max = 220, message = "Slug cannot exceed 220 characters")
    @Schema(description = "URL-friendly slug (auto-generated from name if blank)", example = "wireless-studio-earbuds-pro")
    private String slug;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    @Schema(description = "Comprehensive product description", example = "High fidelity wireless earbuds with 32h playback")
    private String description;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.01", message = "Cost price must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Cost price cannot exceed 10 integer digits and 2 decimal places")
    @Schema(description = "Internal cost of goods sold (COGS)", example = "35.50")
    private BigDecimal costPrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.01", message = "Selling price must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Selling price cannot exceed 10 integer digits and 2 decimal places")
    @Schema(description = "Retail selling price charged to customer", example = "89.99")
    private BigDecimal sellingPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Schema(description = "Available inventory quantity", example = "50")
    private Integer stockQuantity;

    @NotNull(message = "Low stock threshold is required")
    @Min(value = 0, message = "Low stock threshold cannot be negative")
    @Schema(description = "Threshold that triggers a low-stock alert", example = "5")
    private Integer lowStockThreshold = 5;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    @Schema(description = "URL to product thumbnail or banner image", example = "https://images.unsplash.com/photo-example")
    private String imageUrl;

    @NotNull(message = "Category ID is required")
    @Schema(description = "ID of the associated category", example = "2")
    private Long categoryId;

    @Schema(description = "Active status", example = "true")
    private Boolean isActive = true;

    public ProductRequestDTO() {
    }

    public ProductRequestDTO(String sku, String name, String slug, String description,
                             BigDecimal costPrice, BigDecimal sellingPrice,
                             Integer stockQuantity, Integer lowStockThreshold,
                             String imageUrl, Long categoryId, Boolean isActive) {
        this.sku = sku;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.stockQuantity = stockQuantity;
        this.lowStockThreshold = lowStockThreshold != null ? lowStockThreshold : 5;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.isActive = isActive != null ? isActive : true;
    }

    // Getters and Setters
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
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

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
