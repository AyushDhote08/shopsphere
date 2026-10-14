package com.shopsphere.service;

import com.shopsphere.dto.order.CheckoutRequestDTO;
import com.shopsphere.dto.order.OrderResponseDTO;
import com.shopsphere.entity.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.InsufficientStockException;
import com.shopsphere.exception.PaymentFailedException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.*;
import com.shopsphere.service.impl.OrderServiceImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private InventoryAuditLogRepository inventoryAuditLogRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Cart cart;
    private Product product;
    private Address address;

    @BeforeEach
    void setUp() {
        user = new User("customer@example.com", "hash", "Bob Jones", "+111222333", Role.ROLE_CUSTOMER);
        user.setId(2L);

        cart = new Cart(user);
        cart.setId(20L);

        Category category = new Category("Electronics", "electronics", "Tech", null);
        product = new Product(
                "SKU-MON-01", "4K HDR Monitor", "4k-hdr-monitor", "Ultra-clear display",
                new BigDecimal("200.00"), new BigDecimal("399.99"), 10, 2,
                "https://example.com/mon.jpg", category
        );
        product.setId(201L);

        address = new Address(user, "Bob Jones", "+111222333", "123 Elm St", "Springfield", "IL", "62701", "United States", true);
        address.setId(10L);
    }

    @Test
    @DisplayName("checkout: Should successfully create order, deduct stock, create snapshots, and clear cart")
    void shouldCompleteCheckoutSuccessfully() {
        CartItem cartItem = new CartItem(cart, product, 2);
        cartItem.setId(1L);

        CheckoutRequestDTO request = new CheckoutRequestDTO(10L, PaymentMethod.SIMULATED_CARD, "SUCCESS", "idemp-key-1");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndIdempotencyKey(2L, "idemp-key-1")).thenReturn(Optional.empty());
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(cartItem));
        when(addressService.resolveOrCreateAddress(eq(2L), eq(10L), any())).thenReturn(address);
        when(productRepository.findAllByIdInWithPessimisticLock(List.of(201L))).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1001L);
            return o;
        });

        OrderResponseDTO response = orderService.checkout(2L, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        // Subtotal = 399.99 * 2 = 799.98
        assertThat(response.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("799.98"));
        // Free shipping on >= $100
        assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
        // Inventory deduction: 10 - 2 = 8
        assertThat(product.getStockQuantity()).isEqualTo(8);

        // Verification of side-effects
        verify(productRepository).save(product);
        verify(inventoryAuditLogRepository).save(any(InventoryAuditLog.class));
        verify(paymentAttemptRepository).save(any(PaymentAttempt.class));
        verify(cartItemRepository).deleteAllByCartId(20L);
    }

    @Test
    @DisplayName("checkout: Should throw BadRequestException when cart is empty")
    void shouldThrowWhenCartIsEmpty() {
        CheckoutRequestDTO request = new CheckoutRequestDTO(10L, PaymentMethod.SIMULATED_CARD, "SUCCESS", null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout(2L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot checkout with an empty cart");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("checkout: Should throw InsufficientStockException when product stock is inadequate")
    void shouldThrowWhenStockIsInsufficient() {
        product.setStockQuantity(1); // Only 1 in stock, but cart requests 2
        CartItem cartItem = new CartItem(cart, product, 2);

        CheckoutRequestDTO request = new CheckoutRequestDTO(10L, PaymentMethod.SIMULATED_CARD, "SUCCESS", null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(cartItem));
        when(addressService.resolveOrCreateAddress(eq(2L), eq(10L), any())).thenReturn(address);
        when(productRepository.findAllByIdInWithPessimisticLock(List.of(201L))).thenReturn(List.of(product));

        assertThatThrownBy(() -> orderService.checkout(2L, request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for '4K HDR Monitor'");

        // Verify no inventory changed and cart not cleared
        assertThat(product.getStockQuantity()).isEqualTo(1);
        verify(cartItemRepository, never()).deleteAllByCartId(anyLong());
    }

    @Test
    @DisplayName("checkout: Should throw PaymentFailedException on simulated failure and keep inventory/cart intact")
    void shouldThrowAndRollbackOnPaymentFailure() {
        CartItem cartItem = new CartItem(cart, product, 1);

        CheckoutRequestDTO request = new CheckoutRequestDTO(10L, PaymentMethod.SIMULATED_CARD, "FAILED", null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(cartItem));
        when(addressService.resolveOrCreateAddress(eq(2L), eq(10L), any())).thenReturn(address);
        when(productRepository.findAllByIdInWithPessimisticLock(List.of(201L))).thenReturn(List.of(product));

        assertThatThrownBy(() -> orderService.checkout(2L, request))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("Simulated payment failed");

        // Stock quantity must remain unreduced (10)
        assertThat(product.getStockQuantity()).isEqualTo(10);
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartItemRepository, never()).deleteAllByCartId(anyLong());
    }

    @Test
    @DisplayName("checkout: Should return existing order if idempotency key already processed")
    void shouldReturnExistingOrderForDuplicateSubmission() {
        Order existingOrder = new Order("ORD-20261014-ABCDEF", user, address,
                new BigDecimal("100.00"), new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("105.00"));
        existingOrder.setId(99L);
        existingOrder.setIdempotencyKey("idemp-unique-123");

        CheckoutRequestDTO request = new CheckoutRequestDTO(10L, PaymentMethod.SIMULATED_CARD, "SUCCESS", "idemp-unique-123");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndIdempotencyKey(2L, "idemp-unique-123")).thenReturn(Optional.of(existingOrder));

        OrderResponseDTO res = orderService.checkout(2L, request);

        assertThat(res).isNotNull();
        assertThat(res.getOrderNumber()).isEqualTo("ORD-20261014-ABCDEF");
        // Verify no second charge or duplicate order save
        verify(cartItemRepository, never()).deleteAllByCartId(anyLong());
    }

    @Test
    @DisplayName("getOrderById: Should enforce customer isolation and throw ResourceNotFoundException for foreign order")
    void shouldEnforceOrderIsolation() {
        when(orderRepository.findByIdAndUserId(999L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(2L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: '999'");
    }
}
