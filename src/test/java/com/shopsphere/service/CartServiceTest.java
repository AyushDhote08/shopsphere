package com.shopsphere.service;

import com.shopsphere.dto.cart.CartItemRequestDTO;
import com.shopsphere.dto.cart.CartItemUpdateDTO;
import com.shopsphere.dto.cart.CartResponseDTO;
import com.shopsphere.entity.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.InsufficientStockException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.CartItemRepository;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;
    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        user = new User("alice@example.com", "passHash", "Alice Smith", "+123456789", Role.ROLE_CUSTOMER);
        user.setId(1L);

        cart = new Cart(user);
        cart.setId(10L);

        category = new Category("Audio", "audio", "Audio items", null);
        category.setId(1L);

        product = new Product(
                "SKU-EAR-01", "Studio Wireless Earbuds", "studio-wireless-earbuds",
                "High fidelity earbuds", new BigDecimal("40.00"), new BigDecimal("99.99"),
                10, 2, "https://example.com/earbuds.jpg", category
        );
        product.setId(101L);
    }

    @Test
    @DisplayName("getCart: Should return cart and calculate subtotal, tax, and shipping")
    void shouldReturnCartWithCalculatedTotals() {
        CartItem item = new CartItem(cart, product, 1);
        item.setId(50L);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(item));

        CartResponseDTO res = cartService.getCart(1L);

        assertThat(res).isNotNull();
        assertThat(res.getItems()).hasSize(1);
        assertThat(res.getTotalItems()).isEqualTo(1);
        assertThat(res.getSubtotal()).isEqualByComparingTo(new BigDecimal("99.99"));
        // Subtotal < $100 -> Standard shipping $9.99 applies
        assertThat(res.getShippingFee()).isEqualByComparingTo(new BigDecimal("9.99"));
        // 5% tax of $99.99 = $5.00
        assertThat(res.getTaxAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
        // Total = 99.99 + 5.00 + 9.99 = 114.98
        assertThat(res.getTotalAmount()).isEqualByComparingTo(new BigDecimal("114.98"));
    }

    @Test
    @DisplayName("addItemToCart: Should add new product to cart")
    void shouldAddNewItemToCart() {
        CartItemRequestDTO request = new CartItemRequestDTO(101L, 2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 101L)).thenReturn(Optional.empty());
        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));

        CartItem savedItem = new CartItem(cart, product, 2);
        savedItem.setId(51L);
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(savedItem));

        CartResponseDTO res = cartService.addItemToCart(1L, request);

        verify(cartItemRepository).save(any(CartItem.class));
        assertThat(res.getTotalItems()).isEqualTo(2);
        assertThat(res.getSubtotal()).isEqualByComparingTo(new BigDecimal("199.98"));
        // Subtotal >= $100 -> Free shipping
        assertThat(res.getShippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("addItemToCart: Should aggregate quantity when product already in cart")
    void shouldAggregateExistingItemQuantity() {
        CartItem existingItem = new CartItem(cart, product, 3);
        existingItem.setId(52L);

        CartItemRequestDTO request = new CartItemRequestDTO(101L, 2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 101L)).thenReturn(Optional.of(existingItem));
        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));

        CartItem updatedItem = new CartItem(cart, product, 5);
        updatedItem.setId(52L);
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(updatedItem));

        CartResponseDTO res = cartService.addItemToCart(1L, request);

        assertThat(existingItem.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    @DisplayName("addItemToCart: Should throw InsufficientStockException when quantity exceeds stock")
    void shouldThrowWhenQuantityExceedsAvailableStock() {
        product.setStockQuantity(2); // Only 2 available
        CartItemRequestDTO request = new CartItemRequestDTO(101L, 5);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItemToCart(1L, request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for 'Studio Wireless Earbuds'");

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("addItemToCart: Should throw BadRequestException when product is inactive")
    void shouldThrowWhenProductIsInactive() {
        product.setActive(false);
        CartItemRequestDTO request = new CartItemRequestDTO(101L, 1);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItemToCart(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is currently inactive");
    }

    @Test
    @DisplayName("updateCartItemQuantity: Should throw ResourceNotFoundException when item belongs to another customer")
    void shouldEnforceCustomerIsolationOnUpdate() {
        CartItemUpdateDTO request = new CartItemUpdateDTO(4);

        // Attempting to modify item 999 belonging to another user
        when(cartItemRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(1L, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart item not found in your cart");
    }

    @Test
    @DisplayName("removeCartItem: Should remove item from user's cart")
    void shouldRemoveCartItem() {
        CartItem item = new CartItem(cart, product, 2);
        item.setId(55L);

        when(cartItemRepository.findByIdAndUserId(55L, 1L)).thenReturn(Optional.of(item));
        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of());

        CartResponseDTO res = cartService.removeCartItem(1L, 55L);

        verify(cartItemRepository).delete(item);
        assertThat(res.getItems()).isEmpty();
        assertThat(res.getTotalItems()).isEqualTo(0);
        assertThat(res.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
