package com.shopsphere.controller;

import com.shopsphere.dto.admin.InventoryMovementResponseDTO;
import com.shopsphere.dto.admin.RestockRequestDTO;
import com.shopsphere.dto.admin.StockAdjustmentRequestDTO;
import com.shopsphere.dto.admin.UpdateReorderThresholdDTO;
import com.shopsphere.dto.common.ApiResponse;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.product.ProductResponseDTO;
import com.shopsphere.security.SecurityUtils;
import com.shopsphere.service.AdminInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory")
@Tag(name = "Admin Inventory Management", description = "Endpoints for stock level tracking, restocking, adjustments, and movement audit logs")
public class AdminInventoryController {

    private final AdminInventoryService inventoryService;
    private final SecurityUtils securityUtils;

    public AdminInventoryController(AdminInventoryService inventoryService, SecurityUtils securityUtils) {
        this.inventoryService = inventoryService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Operation(summary = "Get inventory stock levels", description = "Retrieves paginated product inventory with optional search and low-stock filter")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDTO>>> getProductsInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "false") Boolean lowStockOnly) {
        PageResponse<ProductResponseDTO> response = inventoryService.getProductsInventory(page, size, query, lowStockOnly);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low-stock products", description = "Retrieves products where stock is at or below the reorder threshold")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDTO>>> getLowStockProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<ProductResponseDTO> response = inventoryService.getLowStockProducts(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/products/{id}/threshold")
    @Operation(summary = "Configure reorder threshold", description = "Updates the low-stock alert threshold for a specific product")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateReorderThreshold(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReorderThresholdDTO request) {
        ProductResponseDTO updated = inventoryService.updateReorderThreshold(id, request);
        return ResponseEntity.ok(ApiResponse.success("Reorder threshold updated successfully", updated));
    }

    @PostMapping("/restock")
    @Operation(summary = "Restock product inventory", description = "Executes transactional inventory addition with audit logging")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> restockProduct(
            @Valid @RequestBody RestockRequestDTO request) {
        Long adminUserId = securityUtils.getCurrentUserId();
        ProductResponseDTO updated = inventoryService.restockProduct(adminUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Product restocked successfully", updated));
    }

    @PostMapping("/adjustments")
    @Operation(summary = "Manual stock adjustment", description = "Executes manual stock increment/decrement with mandatory justification reason")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> adjustStock(
            @Valid @RequestBody StockAdjustmentRequestDTO request) {
        Long adminUserId = securityUtils.getCurrentUserId();
        ProductResponseDTO updated = inventoryService.adjustStock(adminUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", updated));
    }

    @GetMapping("/movements")
    @Operation(summary = "Get inventory movement history", description = "Retrieves paginated historical stock changes, reasons, and responsible administrator")
    public ResponseEntity<ApiResponse<PageResponse<InventoryMovementResponseDTO>>> getInventoryMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String movementType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<InventoryMovementResponseDTO> response = inventoryService.getInventoryMovements(productId, movementType, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
