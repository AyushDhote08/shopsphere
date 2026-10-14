package com.shopsphere.controller;

import com.shopsphere.dto.admin.AdminOrderResponseDTO;
import com.shopsphere.dto.admin.AdminOrderStatusUpdateDTO;
import com.shopsphere.dto.common.ApiResponse;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.entity.OrderStatus;
import com.shopsphere.security.SecurityUtils;
import com.shopsphere.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin Order Management", description = "Endpoints for managing customer orders, viewing line items, and updating fulfillment status")
public class AdminOrderController {

    private final AdminOrderService orderService;
    private final SecurityUtils securityUtils;

    public AdminOrderController(AdminOrderService orderService, SecurityUtils securityUtils) {
        this.orderService = orderService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Operation(summary = "Get all customer orders", description = "Retrieves paginated orders with filters for status, tracking number, and date range")
    public ResponseEntity<ApiResponse<PageResponse<AdminOrderResponseDTO>>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<AdminOrderResponseDTO> response = orderService.getOrders(status, orderNumber, startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID", description = "Retrieves comprehensive administrative details, customer info, and snapshot line items")
    public ResponseEntity<ApiResponse<AdminOrderResponseDTO>> getOrderById(@PathVariable Long id) {
        AdminOrderResponseDTO response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order fulfillment status", description = "Enforces controlled state machine transitions (e.g. CONFIRMED to SHIPPED)")
    public ResponseEntity<ApiResponse<AdminOrderResponseDTO>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminOrderStatusUpdateDTO request) {
        Long adminUserId = securityUtils.getCurrentUserId();
        AdminOrderResponseDTO response = orderService.updateOrderStatus(adminUserId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", response));
    }
}
