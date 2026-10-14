package com.shopsphere.service;

import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.product.ProductRequestDTO;
import com.shopsphere.dto.product.ProductResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    PageResponse<ProductResponseDTO> getProducts(
            String query,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    ProductResponseDTO getProductById(Long id);

    ProductResponseDTO getProductBySlug(String slug);

    ProductResponseDTO getProductBySku(String sku);

    ProductResponseDTO createProduct(ProductRequestDTO requestDTO);

    ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO);

    void deleteProduct(Long id);

    List<ProductResponseDTO> getLowStockProducts();
}
