package com.shopsphere.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.admin.AdminOrderStatusUpdateDTO;
import com.shopsphere.dto.admin.RestockRequestDTO;
import com.shopsphere.dto.admin.StockAdjustmentRequestDTO;
import com.shopsphere.dto.admin.UpdateReorderThresholdDTO;
import com.shopsphere.entity.OrderStatus;
import com.shopsphere.service.AdminDashboardService;
import com.shopsphere.service.AdminInventoryService;
import com.shopsphere.service.AdminOrderService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private AdminInventoryService adminInventoryService;

    @MockBean
    private AdminOrderService adminOrderService;

    @MockBean
    private SecurityUtils securityUtils;

    // =========================================================================
    // 1. ANONYMOUS USERS (Unauthenticated) -> 401 UNAUTHORIZED
    // =========================================================================

    @Test
    @DisplayName("Anonymous: Accessing GET /api/admin/dashboard -> 401 Unauthorized")
    void shouldRejectAnonymousDashboardAccess() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Anonymous: Accessing GET /api/admin/inventory -> 401 Unauthorized")
    void shouldRejectAnonymousInventoryAccess() throws Exception {
        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Anonymous: Accessing GET /api/admin/orders -> 401 Unauthorized")
    void shouldRejectAnonymousOrdersAccess() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Anonymous: Accessing POST /api/admin/inventory/restock -> 401 Unauthorized")
    void shouldRejectAnonymousRestock() throws Exception {
        RestockRequestDTO request = new RestockRequestDTO(1L, 10, "Restock attempt");

        mockMvc.perform(post("/api/admin/inventory/restock")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 2. CUSTOMER USERS (ROLE_CUSTOMER) -> 403 FORBIDDEN
    // =========================================================================

    @Test
    @DisplayName("Customer: Accessing GET /api/admin/dashboard -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldRejectCustomerDashboardAccess() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Customer: Accessing GET /api/admin/inventory -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldRejectCustomerInventoryAccess() throws Exception {
        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Customer: Accessing GET /api/admin/orders -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldRejectCustomerOrdersAccess() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Customer: Accessing POST /api/admin/inventory/restock -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldRejectCustomerRestock() throws Exception {
        RestockRequestDTO request = new RestockRequestDTO(1L, 10, "Restock attempt");

        mockMvc.perform(post("/api/admin/inventory/restock")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Customer: Accessing PATCH /api/admin/orders/1/status -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldRejectCustomerOrderStatusUpdate() throws Exception {
        AdminOrderStatusUpdateDTO request = new AdminOrderStatusUpdateDTO(OrderStatus.DELIVERED, null);

        mockMvc.perform(patch("/api/admin/orders/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. ADMIN USERS (ROLE_ADMIN) -> 200 OK / AUTHORIZED
    // =========================================================================

    @Test
    @DisplayName("Admin: Accessing GET /api/admin/dashboard -> 200 OK")
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void shouldAllowAdminDashboardAccess() throws Exception {
        when(adminDashboardService.getDashboardSummary(any(), any())).thenReturn(new com.shopsphere.dto.admin.AdminDashboardSummaryDTO());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin: Accessing GET /api/admin/inventory -> 200 OK")
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void shouldAllowAdminInventoryAccess() throws Exception {
        when(adminInventoryService.getProductsInventory(anyInt(), anyInt(), any(), any())).thenReturn(new com.shopsphere.dto.common.PageResponse<>());

        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin: Accessing GET /api/admin/orders -> 200 OK")
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void shouldAllowAdminOrdersAccess() throws Exception {
        when(adminOrderService.getOrders(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(new com.shopsphere.dto.common.PageResponse<>());

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin: Accessing POST /api/admin/inventory/restock with CSRF -> 200 OK")
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void shouldAllowAdminRestockWithCsrf() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        RestockRequestDTO request = new RestockRequestDTO(1L, 10, "Restock attempt");
        when(adminInventoryService.restockProduct(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/admin/inventory/restock")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin: Accessing POST /api/admin/inventory/adjustments with CSRF -> 200 OK")
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void shouldAllowAdminAdjustmentWithCsrf() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        StockAdjustmentRequestDTO request = new StockAdjustmentRequestDTO(1L, 5, "Warehouse count discrepancy");
        when(adminInventoryService.adjustStock(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/admin/inventory/adjustments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin: Accessing PATCH /api/admin/inventory/products/1/threshold with CSRF -> 200 OK")
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void shouldAllowAdminThresholdUpdateWithCsrf() throws Exception {
        UpdateReorderThresholdDTO request = new UpdateReorderThresholdDTO(8);
        when(adminInventoryService.updateReorderThreshold(anyLong(), any())).thenReturn(null);

        mockMvc.perform(patch("/api/admin/inventory/products/1/threshold")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin: State-changing POST without CSRF -> 403 Forbidden")
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void shouldRejectAdminPostWithoutCsrf() throws Exception {
        RestockRequestDTO request = new RestockRequestDTO(1L, 10, "Restock attempt");

        mockMvc.perform(post("/api/admin/inventory/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
