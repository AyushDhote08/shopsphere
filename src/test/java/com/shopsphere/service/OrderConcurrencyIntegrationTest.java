package com.shopsphere.service;

import com.shopsphere.dto.cart.CartItemRequestDTO;
import com.shopsphere.dto.order.CheckoutRequestDTO;
import com.shopsphere.dto.order.OrderResponseDTO;
import com.shopsphere.entity.*;
import com.shopsphere.exception.InsufficientStockException;
import com.shopsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    private Long productId;
    private Long userAId;
    private Long userBId;
    private Long addressAId;
    private Long addressBId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Category
        Category category = new Category("Electronics", "electronics", "Tech", null);
        category = categoryRepository.save(category);

        // Product with ONLY 1 unit in stock
        Product product = new Product(
                "SKU-CONC-01", "Limited Edition GPU", "limited-edition-gpu", "Single unit",
                new BigDecimal("500.00"), new BigDecimal("999.00"), 1, 1,
                "https://example.com/gpu.jpg", category
        );
        product = productRepository.save(product);
        productId = product.getId();

        // Customer A
        User userA = new User("user.a@example.com", "$2a$12$hashA", "User A", "111", Role.ROLE_CUSTOMER);
        userA = userRepository.save(userA);
        userAId = userA.getId();

        Address addrA = new Address(userA, "User A", "111", "Street A", "City", "ST", "12345", "US", true);
        addrA = addressRepository.save(addrA);
        addressAId = addrA.getId();

        // Customer B
        User userB = new User("user.b@example.com", "$2a$12$hashB", "User B", "222", Role.ROLE_CUSTOMER);
        userB = userRepository.save(userB);
        userBId = userB.getId();

        Address addrB = new Address(userB, "User B", "222", "Street B", "City", "ST", "12345", "US", true);
        addrB = addressRepository.save(addrB);
        addressBId = addrB.getId();

        // Both customers add the final available unit (qty = 1) to their respective carts
        cartService.addItemToCart(userAId, new CartItemRequestDTO(productId, 1));
        cartService.addItemToCart(userBId, new CartItemRequestDTO(productId, 1));
    }

    @Test
    @DisplayName("Concurrency: Simultaneous checkout for final available unit must prevent overselling")
    void shouldPreventOversellingUnderConcurrentCheckout() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);

        // Thread 1: Customer A checkout
        executor.submit(() -> {
            try {
                startLatch.await();
                CheckoutRequestDTO reqA = new CheckoutRequestDTO(addressAId, PaymentMethod.SIMULATED_CARD, "SUCCESS", "idemp-a");
                OrderResponseDTO order = orderService.checkout(userAId, reqA);
                if (order != null && order.getOrderStatus() == OrderStatus.CONFIRMED) {
                    successCount.incrementAndGet();
                }
            } catch (InsufficientStockException ex) {
                insufficientStockCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Customer B checkout
        executor.submit(() -> {
            try {
                startLatch.await();
                CheckoutRequestDTO reqB = new CheckoutRequestDTO(addressBId, PaymentMethod.SIMULATED_CARD, "SUCCESS", "idemp-b");
                OrderResponseDTO order = orderService.checkout(userBId, reqB);
                if (order != null && order.getOrderStatus() == OrderStatus.CONFIRMED) {
                    successCount.incrementAndGet();
                }
            } catch (InsufficientStockException ex) {
                insufficientStockCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        // Fire both threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // Exactly ONE checkout succeeded, and ONE failed due to insufficient stock
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(insufficientStockCount.get()).isEqualTo(1);

        // Database stock must be exactly 0 (NOT -1)
        Product finalProduct = productRepository.findById(productId).orElseThrow();
        assertThat(finalProduct.getStockQuantity()).isEqualTo(0);
    }
}
