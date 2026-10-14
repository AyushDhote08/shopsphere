package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.cart.CartItemRequestDTO;
import com.shopsphere.dto.cart.CartItemResponseDTO;
import com.shopsphere.dto.cart.CartItemUpdateDTO;
import com.shopsphere.dto.cart.CartResponseDTO;
import com.shopsphere.security.SecurityUtils;
import com.shopsphere.service.CartService;
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
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private SecurityUtils securityUtils;

    @Test
    @DisplayName("GET /api/cart: Should return 200 with cart for authenticated customer")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldReturnCartWhenAuthenticated() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        CartResponseDTO cartDTO = new CartResponseDTO();
        cartDTO.setId(10L);
        cartDTO.setTotalItems(1);
        cartDTO.setSubtotal(new BigDecimal("99.99"));
        cartDTO.setTotalAmount(new BigDecimal("114.98"));

        CartItemResponseDTO item = new CartItemResponseDTO();
        item.setId(5L);
        item.setProductId(101L);
        item.setProductName("Studio Earbuds");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("99.99"));
        item.setLineTotal(new BigDecimal("99.99"));
        cartDTO.setItems(List.of(item));

        when(cartService.getCart(1L)).thenReturn(cartDTO);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.subtotal").value(99.99))
                .andExpect(jsonPath("$.data.totalAmount").value(114.98));
    }

    @Test
    @DisplayName("GET /api/cart: Should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/cart/items: Should add product to cart with CSRF token")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldAddItemToCart() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        CartItemRequestDTO request = new CartItemRequestDTO(101L, 2);

        CartResponseDTO cartDTO = new CartResponseDTO();
        cartDTO.setId(10L);
        cartDTO.setTotalItems(2);
        cartDTO.setSubtotal(new BigDecimal("199.98"));

        when(cartService.addItemToCart(eq(1L), any(CartItemRequestDTO.class))).thenReturn(cartDTO);

        mockMvc.perform(post("/api/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(2));
    }

    @Test
    @DisplayName("PATCH /api/cart/items/{itemId}: Should update item quantity")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldUpdateItemQuantity() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        CartItemUpdateDTO request = new CartItemUpdateDTO(3);

        CartResponseDTO cartDTO = new CartResponseDTO();
        cartDTO.setId(10L);
        cartDTO.setTotalItems(3);

        when(cartService.updateCartItemQuantity(eq(1L), eq(5L), any(CartItemUpdateDTO.class))).thenReturn(cartDTO);

        mockMvc.perform(patch("/api/cart/items/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(3));
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{itemId}: Should remove item from cart")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldRemoveCartItem() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        CartResponseDTO cartDTO = new CartResponseDTO();
        cartDTO.setId(10L);
        cartDTO.setTotalItems(0);

        when(cartService.removeCartItem(1L, 5L)).thenReturn(cartDTO);

        mockMvc.perform(delete("/api/cart/items/5")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/cart/items: Should clear cart")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void shouldClearCart() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        doNothing().when(cartService).clearCart(1L);

        mockMvc.perform(delete("/api/cart/items")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cart cleared successfully"));
    }
}
