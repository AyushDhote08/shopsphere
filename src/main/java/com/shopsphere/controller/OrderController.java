package com.shopsphere.controller;

import com.shopsphere.dto.common.ApiResponse;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.order.CheckoutRequestDTO;
import com.shopsphere.dto.order.OrderResponseDTO;
import com.shopsphere.security.SecurityUtils;
import com.shopsphere.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "Endpoints for customer checkout, simulated payment, and order history")
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    public OrderController(OrderService orderService, SecurityUtils securityUtils) {
        this.orderService = orderService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/checkout")
    @Operation(summary = "Process checkout", description = "Executes transactional checkout with pessimistic inventory locking and simulated payment")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> checkout(@Valid @RequestBody CheckoutRequestDTO request) {
        Long userId = securityUtils.getCurrentUserId();
        OrderResponseDTO order = orderService.checkout(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", order));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my orders", description = "Retrieves paginated order history for the authenticated customer")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponseDTO>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = securityUtils.getCurrentUserId();
        PageResponse<OrderResponseDTO> orders = orderService.getMyOrders(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves order details for an order owned by the authenticated customer")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        OrderResponseDTO order = orderService.getOrderById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number", description = "Retrieves order details by public order number for the authenticated customer")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderByOrderNumber(@PathVariable String orderNumber) {
        Long userId = securityUtils.getCurrentUserId();
        OrderResponseDTO order = orderService.getOrderByOrderNumber(userId, orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
