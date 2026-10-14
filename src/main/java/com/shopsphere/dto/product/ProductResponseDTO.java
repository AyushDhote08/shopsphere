package com.shopsphere.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Product response data")
public class ProductResponseDTO {

    @Schema(description = "Unique database ID", example = "1")
    private Long id;

    @Schema(description = "Stock Keeping Unit", example = "ELEC-AUD-001")
    private String sku;

    @Schema(description = "Product display title", example = "AcousticPro ANC Wireless Headphones")
    private String name;

    @Schema(description = "URL slug", example = "acousticpro-anc-wireless-headphones")
    private String slug;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Internal cost of goods sold", example = "85.00")
    private BigDecimal costPrice;

    @Schema(description = "Retail selling price", example = "199.99")
    private BigDecimal sellingPrice;

    @Schema(description = "Current stock level", example = "45")
    private Integer stockQuantity;

    @Schema(description = "Low stock alert threshold", example = "5")
    private Integer lowStockThreshold;

    @Schema(description = "Image URL", example = "https://images.unsplash.com/photo-example")
    private String imageUrl;

    @Schema(description = "Associated category ID", example = "2")
    private Long categoryId;

    @Schema(description = "Associated category name", example = "Audio & Sound")
    private String categoryName;

    @Schema(description = "Active status", example = "true")
    private boolean isActive;

    @Schema(description = "Whether the product is currently in stock", example = "true")
    private boolean inStock;

    @Schema(description = "Whether the product is at or below the low stock threshold", example = "false")
    private boolean lowStock;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public ProductResponseDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    public boolean isLowStock() {
        return lowStock;
    }

    public void setLowStock(boolean lowStock) {
        this.lowStock = lowStock;
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
