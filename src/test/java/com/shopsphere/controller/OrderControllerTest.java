package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.order.CheckoutRequestDTO;
import com.shopsphere.dto.order.OrderResponseDTO;
import com.shopsphere.entity.OrderStatus;
import com.shopsphere.entity.PaymentMethod;
import com.shopsphere.entity.PaymentStatus;
import com.shopsphere.security.SecurityUtils;
import com.shopsphere.service.OrderService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private SecurityUtils securityUtils;

    @Test
    @DisplayName("POST /api/orders/checkout: Should create order and return 201 for authenticated customer")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldCreateOrderOnCheckout() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(2L);

        CheckoutRequestDTO request = new CheckoutRequestDTO(1L, PaymentMethod.SIMULATED_CARD, "SUCCESS", "idemp-test");

        OrderResponseDTO orderDTO = new OrderResponseDTO();
        orderDTO.setId(101L);
        orderDTO.setOrderNumber("ORD-20261014-ABCDEF");
        orderDTO.setOrderStatus(OrderStatus.CONFIRMED);
        orderDTO.setPaymentStatus(PaymentStatus.PAID);
        orderDTO.setTotalAmount(new BigDecimal("199.99"));
        orderDTO.setCreatedAt(LocalDateTime.now());

        when(orderService.checkout(eq(2L), any(CheckoutRequestDTO.class))).thenReturn(orderDTO);

        mockMvc.perform(post("/api/orders/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-20261014-ABCDEF"))
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.totalAmount").value(199.99));
    }

    @Test
    @DisplayName("POST /api/orders/checkout: Should return 401 when unauthenticated")
    void shouldReturn401WhenCheckoutUnauthenticated() throws Exception {
        CheckoutRequestDTO request = new CheckoutRequestDTO(1L, PaymentMethod.SIMULATED_CARD, "SUCCESS", null);

        mockMvc.perform(post("/api/orders/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/orders/my: Should return customer's order history")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldReturnCustomerOrderHistory() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(2L);

        OrderResponseDTO order = new OrderResponseDTO();
        order.setId(101L);
        order.setOrderNumber("ORD-20261014-ABCDEF");
        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("199.99"));

        PageResponse<OrderResponseDTO> pageResponse = new PageResponse<>(
                List.of(order), 0, 10, 1, 1, true, true, false, false
        );

        when(orderService.getMyOrders(2L, 0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/orders/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-20261014-ABCDEF"));
    }

    @Test
    @DisplayName("GET /api/orders/{id}: Should return specific order for customer")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldReturnOrderById() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(2L);

        OrderResponseDTO order = new OrderResponseDTO();
        order.setId(101L);
        order.setOrderNumber("ORD-20261014-ABCDEF");
        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("199.99"));

        when(orderService.getOrderById(2L, 101L)).thenReturn(order);

        mockMvc.perform(get("/api/orders/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-20261014-ABCDEF"));
    }
}
