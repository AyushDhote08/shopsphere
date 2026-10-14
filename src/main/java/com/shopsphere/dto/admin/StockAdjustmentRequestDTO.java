package com.shopsphere.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StockAdjustmentRequestDTO {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity adjustment is required")
    private Integer quantityAdjustment; // positive to increase, negative to decrease

    @NotBlank(message = "Adjustment reason is mandatory")
    @Size(min = 3, max = 500, message = "Reason must be between 3 and 500 characters")
    private String reason;

    @Size(max = 100, message = "Reference ID cannot exceed 100 characters")
    private String referenceId;

    public StockAdjustmentRequestDTO() {
    }

    public StockAdjustmentRequestDTO(Long productId, Integer quantityAdjustment, String reason) {
        this(productId, quantityAdjustment, reason, null);
    }

    public StockAdjustmentRequestDTO(Long productId, Integer quantityAdjustment, String reason, String referenceId) {
        this.productId = productId;
        this.quantityAdjustment = quantityAdjustment;
        this.reason = reason;
        this.referenceId = referenceId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantityAdjustment() {
        return quantityAdjustment;
    }

    public void setQuantityAdjustment(Integer quantityAdjustment) {
        this.quantityAdjustment = quantityAdjustment;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}
