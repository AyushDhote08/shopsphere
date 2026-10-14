package com.shopsphere.dto.admin;

import com.shopsphere.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminOrderStatusUpdateDTO {

    @NotNull(message = "New order status is required")
    private OrderStatus newStatus;

    @Size(max = 500, message = "Reason/note cannot exceed 500 characters")
    private String reason;

    public AdminOrderStatusUpdateDTO() {
    }

    public AdminOrderStatusUpdateDTO(OrderStatus newStatus, String reason) {
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
