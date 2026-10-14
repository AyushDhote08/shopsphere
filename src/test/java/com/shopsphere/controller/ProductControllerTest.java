package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.product.ProductRequestDTO;
import com.shopsphere.dto.product.ProductResponseDTO;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("GET /api/products: Should return paginated product list")
    void shouldReturnPaginatedProducts() throws Exception {
        ProductResponseDTO item = new ProductResponseDTO();
        item.setId(101L);
        item.setSku("PROD-001");
        item.setName("Wireless Noise-Cancelling Headphones");
        item.setSellingPrice(new BigDecimal("149.99"));

        PageResponse<ProductResponseDTO> pageResponse = new PageResponse<>(
                List.of(item), 0, 12, 1, 1, true, true, false, false
        );

        when(productService.getProducts(any(), any(), any(), any(), any(), eq(0), eq(12), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].sku").value("PROD-001"))
                .andExpect(jsonPath("$.data.content[0].sellingPrice").value(149.99));
    }

    @Test
    @DisplayName("GET /api/products/{id}: Should return product by ID")
    void shouldReturnProductById() throws Exception {
        ProductResponseDTO item = new ProductResponseDTO();
        item.setId(101L);
        item.setName("Studio Headphones");
        item.setSku("PROD-001");

        when(productService.getProductById(101L)).thenReturn(item);

        mockMvc.perform(get("/api/products/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Studio Headphones"));
    }

    @Test
    @DisplayName("GET /api/products/{id}: Should return 404 when product does not exist")
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Product", "id", 999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Product not found with id: '999'")));
    }

    @Test
    @DisplayName("POST /api/products: Should create product successfully and return 201")
    void shouldCreateProductSuccessfully() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO(
                "SKU-NEW-1", "Gaming Mouse", "gaming-mouse", "Ergonomic gaming mouse",
                new BigDecimal("25.00"), new BigDecimal("49.99"),
                50, 5, "https://example.com/mouse.jpg", 1L, true
        );

        ProductResponseDTO response = new ProductResponseDTO();
        response.setId(201L);
        response.setSku("SKU-NEW-1");
        response.setName("Gaming Mouse");
        response.setSellingPrice(new BigDecimal("49.99"));

        when(productService.createProduct(any(ProductRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(201))
                .andExpect(jsonPath("$.data.sku").value("SKU-NEW-1"));
    }

    @Test
    @DisplayName("POST /api/products: Should return 400 when validation constraints fail (negative price / missing SKU)")
    void shouldReturn400OnValidationFailure() throws Exception {
        ProductRequestDTO invalidRequest = new ProductRequestDTO(
                "", // Blank SKU
                "A", // Too short name
                null, null,
                new BigDecimal("-5.00"), // Negative cost price
                new BigDecimal("0.00"), // Zero selling price
                -1, // Negative stock
                -1, // Negative threshold
                null, null, true // Missing categoryId
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    @DisplayName("POST /api/products: Should return 409 Conflict when SKU already exists")
    void shouldReturn409OnDuplicateSku() throws Exception {
        ProductRequestDTO duplicateRequest = new ProductRequestDTO(
                "SKU-EXISTS", "Duplicate Product", null, null,
                new BigDecimal("10.00"), new BigDecimal("20.00"),
                10, 2, null, 1L, true
        );

        when(productService.createProduct(any(ProductRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("Product", "sku", "SKU-EXISTS"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("GET /api/products/low-stock: Should return low stock products")
    void shouldReturnLowStockProducts() throws Exception {
        ProductResponseDTO lowStockItem = new ProductResponseDTO();
        lowStockItem.setId(102L);
        lowStockItem.setSku("SKU-LOW-1");
        lowStockItem.setStockQuantity(2);
        lowStockItem.setLowStock(true);

        when(productService.getLowStockProducts()).thenReturn(List.of(lowStockItem));

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].lowStock").value(true));
    }

    @Test
    @DisplayName("DELETE /api/products/{id}: Should return 204 No Content on soft delete")
    void shouldSoftDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct(101L);

        mockMvc.perform(delete("/api/products/101"))
                .andExpect(status().isNoContent());
    }
}
