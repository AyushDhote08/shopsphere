package com.shopsphere.service;

import com.shopsphere.dto.admin.AdminOrderResponseDTO;
import com.shopsphere.dto.admin.AdminOrderStatusUpdateDTO;
import com.shopsphere.dto.admin.SalesAnalyticsDTO;
import com.shopsphere.dto.cart.CartItemRequestDTO;
import com.shopsphere.dto.cart.CartResponseDTO;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.order.CheckoutRequestDTO;
import com.shopsphere.dto.order.OrderResponseDTO;
import com.shopsphere.entity.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class WorkflowEndToEndIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AdminOrderService adminOrderService;

    @Autowired
    private AdminDashboardService adminDashboardService;

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
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private InventoryAuditLogRepository inventoryAuditLogRepository;

    private Long productId;
    private Long customerId;
    private Long adminId;
    private Long addressId;

    @BeforeEach
    void setUp() {
        paymentAttemptRepository.deleteAll();
        inventoryAuditLogRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // 1. Setup Category
        Category category = new Category("Electronics", "electronics", "Tech and peripherals", null);
        category = categoryRepository.save(category);

        // 2. Setup Product (Initial stock: 10 units, cost: $40.00, price: $100.00)
        Product product = new Product(
                "SKU-KB-E2E", "Mechanical Gaming Keyboard", "mechanical-gaming-keyboard",
                "RGB Mechanical Switches", new BigDecimal("40.00"), new BigDecimal("100.00"),
                10, 2, "https://example.com/kb.jpg", category
        );
        product = productRepository.save(product);
        productId = product.getId();

        // 3. Setup Customer User
        User customer = new User("charlie@example.com", "$2a$12$hashCustomer", "Charlie Parker", "+1-555-0101", Role.ROLE_CUSTOMER);
        customer = userRepository.save(customer);
        customerId = customer.getId();

        // 4. Setup Admin User
        User admin = new User("admin@shopsphere.com", "$2a$12$hashAdmin", "Administrator", "+1-555-0999", Role.ROLE_ADMIN);
        admin = userRepository.save(admin);
        adminId = admin.getId();

        // 5. Setup Address
        Address address = new Address(
                customer, "Charlie Parker", "+1-555-0101", "742 Evergreen Terrace",
                "Springfield", "OR", "97477", "USA", true
        );
        address = addressRepository.save(address);
        addressId = address.getId();
    }

    @Test
    @DisplayName("End-to-End Workflow: Add to cart -> Checkout -> Admin lifecycle -> Analytics calculation")
    void fullBusinessWorkflow_Success() {
        // Step 1: Customer adds 2 units to cart
        CartResponseDTO cart = cartService.addItemToCart(customerId, new CartItemRequestDTO(productId, 2));
        assertThat(cart.getTotalItems()).isEqualTo(2);
        assertThat(cart.getSubtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(cart.getTaxAmount()).isEqualByComparingTo(new BigDecimal("10.00")); // 5% of 200
        assertThat(cart.getShippingFee()).isEqualByComparingTo(BigDecimal.ZERO); // Free shipping >= $100
        assertThat(cart.getTotalAmount()).isEqualByComparingTo(new BigDecimal("210.00"));

        // Step 2: Customer executes checkout with simulated card payment
        CheckoutRequestDTO checkoutReq = new CheckoutRequestDTO(
                addressId, PaymentMethod.SIMULATED_CARD, "SUCCESS", "idemp-workflow-01"
        );
        OrderResponseDTO placedOrder = orderService.checkout(customerId, checkoutReq);

        assertThat(placedOrder).isNotNull();
        assertThat(placedOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(placedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(placedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("210.00"));
        assertThat(placedOrder.getItems()).hasSize(1);
        assertThat(placedOrder.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(placedOrder.getItems().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("100.00"));

        // Step 3: Verify inventory decremented from 10 to 8
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(8);

        // Step 4: Verify customer cart is cleared
        CartResponseDTO clearedCart = cartService.getCart(customerId);
        assertThat(clearedCart.getItems()).isEmpty();
        assertThat(clearedCart.getTotalItems()).isEqualTo(0);

        // Step 5: Verify customer order history
        PageResponse<OrderResponseDTO> customerOrders = orderService.getMyOrders(customerId, 0, 10);
        assertThat(customerOrders.getTotalElements()).isEqualTo(1L);
        assertThat(customerOrders.getContent().get(0).getOrderNumber()).isEqualTo(placedOrder.getOrderNumber());

        // Step 6: Admin fulfills order: CONFIRMED -> SHIPPED -> DELIVERED
        AdminOrderStatusUpdateDTO shipReq = new AdminOrderStatusUpdateDTO(OrderStatus.SHIPPED, null);
        AdminOrderResponseDTO shipped = adminOrderService.updateOrderStatus(adminId, placedOrder.getId(), shipReq);
        assertThat(shipped.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);

        AdminOrderStatusUpdateDTO deliverReq = new AdminOrderStatusUpdateDTO(OrderStatus.DELIVERED, null);
        AdminOrderResponseDTO delivered = adminOrderService.updateOrderStatus(adminId, placedOrder.getId(), deliverReq);
        assertThat(delivered.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);

        // Step 7: Enforce invalid transition: DELIVERED cannot transition to CANCELLED
        AdminOrderStatusUpdateDTO invalidCancelReq = new AdminOrderStatusUpdateDTO(OrderStatus.CANCELLED, "Too late");
        assertThatThrownBy(() -> adminOrderService.updateOrderStatus(adminId, placedOrder.getId(), invalidCancelReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid order status transition");

        // Step 8: Verify business analytics reflect the delivered order
        LocalDate today = LocalDate.now();
        SalesAnalyticsDTO analytics = adminDashboardService.getSalesAnalytics(today.minusDays(1), today);
        assertThat(analytics.getTotalOrders()).isEqualTo(1L);
        assertThat(analytics.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("210.00"));
        // Subtotal = 200.00, Cost = 2 * 40.00 = 80.00. Gross profit = 200 - 80 = 120.00
        assertThat(analytics.getTotalProfit()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(analytics.getAverageOrderValue()).isEqualByComparingTo(new BigDecimal("210.00"));
    }

    @Test
    @DisplayName("End-to-End Workflow: Order cancellation restores inventory and excludes order from sales analytics")
    void orderCancellation_RestoresInventoryAndExcludesFromAnalytics() {
        // Customer adds 3 units and checks out
        cartService.addItemToCart(customerId, new CartItemRequestDTO(productId, 3));
        CheckoutRequestDTO checkoutReq = new CheckoutRequestDTO(
                addressId, PaymentMethod.SIMULATED_CARD, "SUCCESS", "idemp-workflow-cancel"
        );
        OrderResponseDTO placedOrder = orderService.checkout(customerId, checkoutReq);

        // Stock decreased from 10 to 7
        Product productBeforeCancel = productRepository.findById(productId).orElseThrow();
        assertThat(productBeforeCancel.getStockQuantity()).isEqualTo(7);

        // Admin cancels the PENDING order
        AdminOrderStatusUpdateDTO cancelReq = new AdminOrderStatusUpdateDTO(
                OrderStatus.CANCELLED, "Customer requested order cancellation before shipment"
        );
        AdminOrderResponseDTO cancelledOrder = adminOrderService.updateOrderStatus(adminId, placedOrder.getId(), cancelReq);

        assertThat(cancelledOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.getCancellationReason()).isEqualTo("Customer requested order cancellation before shipment");

        // Verify inventory is restored back to 10
        Product productAfterCancel = productRepository.findById(productId).orElseThrow();
        assertThat(productAfterCancel.getStockQuantity()).isEqualTo(10);

        // Verify inventory audit log recorded the restoration
        List<InventoryAuditLog> logs = inventoryAuditLogRepository.findByProductIdOrderByCreatedAtDesc(productId);
        assertThat(logs).isNotEmpty();
        InventoryAuditLog latestLog = logs.get(0);
        assertThat(latestLog.getMovementType()).isEqualTo("ORDER_CANCELLED_RESTORE");
        assertThat(latestLog.getQuantityChanged()).isEqualTo(3);
        assertThat(latestLog.getStockBefore()).isEqualTo(7);
        assertThat(latestLog.getStockAfter()).isEqualTo(10);

        // Verify sales analytics excludes the cancelled order
        LocalDate today = LocalDate.now();
        SalesAnalyticsDTO analytics = adminDashboardService.getSalesAnalytics(today.minusDays(1), today);
        assertThat(analytics.getTotalOrders()).isEqualTo(0L);
        assertThat(analytics.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(analytics.getTotalProfit()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
