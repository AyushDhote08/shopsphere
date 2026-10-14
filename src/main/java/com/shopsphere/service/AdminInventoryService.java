package com.shopsphere.service;

import com.shopsphere.dto.admin.InventoryMovementResponseDTO;
import com.shopsphere.dto.admin.RestockRequestDTO;
import com.shopsphere.dto.admin.StockAdjustmentRequestDTO;
import com.shopsphere.dto.admin.UpdateReorderThresholdDTO;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.product.ProductResponseDTO;

public interface AdminInventoryService {

    PageResponse<ProductResponseDTO> getProductsInventory(int page, int size, String query, Boolean lowStockOnly);

    PageResponse<ProductResponseDTO> getLowStockProducts(int page, int size);

    ProductResponseDTO updateReorderThreshold(Long productId, UpdateReorderThresholdDTO request);

    ProductResponseDTO restockProduct(Long adminUserId, RestockRequestDTO request);

    ProductResponseDTO adjustStock(Long adminUserId, StockAdjustmentRequestDTO request);

    PageResponse<InventoryMovementResponseDTO> getInventoryMovements(Long productId, String movementType, int page, int size);
}
