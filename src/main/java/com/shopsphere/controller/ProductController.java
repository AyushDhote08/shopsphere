package com.shopsphere.controller;

import com.shopsphere.dto.common.ApiResponse;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.product.ProductRequestDTO;
import com.shopsphere.dto.product.ProductResponseDTO;
import com.shopsphere.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Catalog", description = "Endpoints for browsing, searching, filtering, and managing products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Browse products with search, filters, and pagination",
            description = "Supports full-text keyword search, category filter, price boundaries, in-stock filter, and multi-field sorting")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDTO>>> getProducts(
            @Parameter(description = "Keyword search for title, description, or SKU")
            @RequestParam(required = false) String query,

            @Parameter(description = "Category ID filter")
            @RequestParam(required = false) Long categoryId,

            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Filter only products with available stock")
            @RequestParam(required = false, defaultValue = "false") Boolean inStockOnly,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "12") int size,

            @Parameter(description = "Sort field: 'price', 'name', 'stock', or 'createdAt'")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction: 'ASC' or 'DESC'")
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        PageResponse<ProductResponseDTO> response = productService.getProducts(
                query, categoryId, minPrice, maxPrice, inStockOnly, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves complete product details by unique database ID")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductById(@PathVariable Long id) {
        ProductResponseDTO product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug", description = "Retrieves complete product details by SEO-friendly URL slug")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductBySlug(@PathVariable String slug) {
        ProductResponseDTO product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU", description = "Retrieves complete product details by Stock Keeping Unit")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductBySku(@PathVariable String sku) {
        ProductResponseDTO product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock products", description = "Retrieves all products where current stock is at or below the low stock threshold")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getLowStockProducts() {
        List<ProductResponseDTO> lowStockProducts = productService.getLowStockProducts();
        return ResponseEntity.ok(ApiResponse.success(lowStockProducts));
    }

    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new product with unique SKU, slug, and positive prices")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> createProduct(@Valid @RequestBody ProductRequestDTO requestDTO) {
        ProductResponseDTO created = productService.createProduct(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product by unique ID")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO requestDTO) {
        ProductResponseDTO updated = productService.updateProduct(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate product", description = "Soft-deactivates product to preserve historical order integrity")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
