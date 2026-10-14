package com.shopsphere.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.category.CategoryRequestDTO;
import com.shopsphere.dto.category.CategoryResponseDTO;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.product.ProductRequestDTO;
import com.shopsphere.dto.product.ProductResponseDTO;
import com.shopsphere.service.CategoryService;
import com.shopsphere.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("Public Access: Anonymous users can browse products")
    void shouldAllowAnonymousProductBrowsing() throws Exception {
        when(productService.getProducts(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(new PageResponse<>(Collections.emptyList(), 0, 12, 0, 0, true, true, false, false));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public Access: Anonymous users can browse categories")
    void shouldAllowAnonymousCategoryBrowsing() throws Exception {
        when(categoryService.getAllActiveCategories())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Role Restriction: Anonymous user cannot create product -> 401 Unauthorized")
    void shouldRejectAnonymousProductCreation() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO(
                "SEC-PROD-1", "Test Product", null, "Desc",
                new BigDecimal("10.00"), new BigDecimal("20.00"), 10, 2, null, 1L, true
        );

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Role Restriction: Customer cannot create product -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldRejectCustomerProductCreation() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO(
                "SEC-PROD-2", "Customer Product", null, "Desc",
                new BigDecimal("10.00"), new BigDecimal("20.00"), 10, 2, null, 1L, true
        );

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Role Restriction: Administrator can create product -> 201 Created")
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void shouldAllowAdminProductCreation() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO(
                "SEC-PROD-3", "Admin Product", null, "Desc",
                new BigDecimal("10.00"), new BigDecimal("20.00"), 10, 2, null, 1L, true
        );

        ProductResponseDTO response = new ProductResponseDTO();
        response.setId(301L);
        response.setSku("SEC-PROD-3");
        when(productService.createProduct(any(ProductRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CSRF Protection: Admin state-changing request without CSRF -> 403 Forbidden")
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void shouldRejectAdminRequestWithoutCsrf() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO(
                "SEC-PROD-4", "No CSRF Product", null, "Desc",
                new BigDecimal("10.00"), new BigDecimal("20.00"), 10, 2, null, 1L, true
        );

        // Perform POST without .with(csrf())
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Role Restriction: Customer cannot create category -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldRejectCustomerCategoryCreation() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO("New Cat", null, "Desc", null, true);

        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Role Restriction: Administrator can create category -> 201 Created")
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void shouldAllowAdminCategoryCreation() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO("Admin Cat", null, "Desc", null, true);
        CategoryResponseDTO response = new CategoryResponseDTO();
        response.setId(50L);
        response.setName("Admin Cat");
        when(categoryService.createCategory(any(CategoryRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
