package com.shopsphere.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RestockRequestDTO {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Restock quantity is required")
    @Min(value = 1, message = "Restock quantity must be at least 1")
    private Integer quantity;

    @Size(max = 100, message = "Reference ID cannot exceed 100 characters")
    private String referenceId;

    @Size(max = 500, message = "Reason/notes cannot exceed 500 characters")
    private String reason;

    public RestockRequestDTO() {
    }

    public RestockRequestDTO(Long productId, Integer quantity, String reason) {
        this(productId, quantity, null, reason);
    }

    public RestockRequestDTO(Long productId, Integer quantity, String referenceId, String reason) {
        this.productId = productId;
        this.quantity = quantity;
        this.referenceId = referenceId;
        this.reason = reason;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
