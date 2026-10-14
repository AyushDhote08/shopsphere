package com.shopsphere.service;

import com.shopsphere.dto.admin.AdminOrderResponseDTO;
import com.shopsphere.dto.admin.AdminOrderStatusUpdateDTO;
import com.shopsphere.entity.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.InventoryAuditLogRepository;
import com.shopsphere.repository.OrderRepository;
import com.shopsphere.repository.PaymentAttemptRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.impl.AdminOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryAuditLogRepository inventoryAuditLogRepository;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminOrderServiceImpl adminOrderService;

    private User customer;
    private User admin;
    private Product product;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        customer = new User("alice@example.com", "hash", "Alice Smith", "+123456789", Role.ROLE_CUSTOMER);
        customer.setId(2L);

        admin = new User("admin@shopsphere.com", "hash", "Admin Officer", "+199988877", Role.ROLE_ADMIN);
        admin.setId(1L);

        Category category = new Category("Electronics", "electronics", "Tech", null);
        category.setId(10L);

        product = new Product(
                "SKU-4K-MON", "4K Monitor", "4k-monitor", "4K HDR monitor",
                new BigDecimal("200.00"), new BigDecimal("350.00"), 8, 2,
                "https://example.com/mon.jpg", category
        );
        product.setId(201L);

        Address address = new Address(customer, "Alice Smith", "+123456789", "123 High St", "Springfield", "IL", "62701", "United States", true);
        address.setId(15L);

        order = new Order(
                "ORD-TEST-500", customer, address,
                new BigDecimal("700.00"), new BigDecimal("56.00"), BigDecimal.ZERO, new BigDecimal("756.00")
        );
        order.setId(500L);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.PAID);

        orderItem = new OrderItem(order, product, "4K Monitor", "SKU-4K-MON", new BigDecimal("350.00"), 2, new BigDecimal("700.00"));
        orderItem.setId(10L);
        orderItem.setUnitCostSnapshot(new BigDecimal("200.00"));

        List<OrderItem> items = new ArrayList<>();
        items.add(orderItem);
        order.setItems(items);
    }

    @Test
    @DisplayName("updateOrderStatus: CONFIRMED -> SHIPPED updates status without restoring inventory")
    void shouldTransitionFromConfirmedToShipped() {
        AdminOrderStatusUpdateDTO request = new AdminOrderStatusUpdateDTO(
                OrderStatus.SHIPPED, "Shipped via FedEx Priority"
        );

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminOrderResponseDTO response = adminOrderService.updateOrderStatus(1L, 500L, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);

        // No inventory restoration on shipping
        verify(productRepository, never()).findByIdWithPessimisticLock(any());
        verify(inventoryAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateOrderStatus: SHIPPED -> DELIVERED updates status successfully")
    void shouldTransitionFromShippedToDelivered() {
        order.setOrderStatus(OrderStatus.SHIPPED);
        AdminOrderStatusUpdateDTO request = new AdminOrderStatusUpdateDTO(
                OrderStatus.DELIVERED, "Delivered to customer front porch"
        );

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminOrderResponseDTO response = adminOrderService.updateOrderStatus(1L, 500L, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("updateOrderStatus: CONFIRMED -> CANCELLED restores inventory and records audit log")
    void shouldRestoreInventoryOnCancellation() {
        AdminOrderStatusUpdateDTO request = new AdminOrderStatusUpdateDTO(
                OrderStatus.CANCELLED, "Customer requested cancellation before shipment"
        );

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(productRepository.findByIdWithPessimisticLock(201L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminOrderResponseDTO response = adminOrderService.updateOrderStatus(1L, 500L, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getCancellationReason()).isEqualTo("Customer requested cancellation before shipment");

        // Stock restored: 8 + 2 = 10
        assertThat(product.getStockQuantity()).isEqualTo(10);
        verify(productRepository).save(product);

        ArgumentCaptor<InventoryAuditLog> logCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(inventoryAuditLogRepository).save(logCaptor.capture());

        InventoryAuditLog log = logCaptor.getValue();
        assertThat(log.getMovementType()).isEqualTo("ORDER_CANCELLED_RESTORE");
        assertThat(log.getQuantityChanged()).isEqualTo(2);
        assertThat(log.getStockBefore()).isEqualTo(8);
        assertThat(log.getStockAfter()).isEqualTo(10);
        assertThat(log.getReason()).contains("Order cancellation restore");
    }

    @Test
    @DisplayName("updateOrderStatus: DELIVERED -> CANCELLED rejected as invalid transition")
    void shouldRejectCancellationOfDeliveredOrder() {
        order.setOrderStatus(OrderStatus.DELIVERED);
        AdminOrderStatusUpdateDTO request = new AdminOrderStatusUpdateDTO(
                OrderStatus.CANCELLED, "Try to cancel delivered order"
        );

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminOrderService.updateOrderStatus(1L, 500L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid order status transition from DELIVERED to CANCELLED");

        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).findByIdWithPessimisticLock(any());
    }

    @Test
    @DisplayName("updateOrderStatus: CANCELLED -> CONFIRMED rejected (terminal state)")
    void shouldRejectTransitionFromTerminalCancelledState() {
        order.setOrderStatus(OrderStatus.CANCELLED);
        AdminOrderStatusUpdateDTO request = new AdminOrderStatusUpdateDTO(
                OrderStatus.CONFIRMED, "Try to revive cancelled order"
        );

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminOrderService.updateOrderStatus(1L, 500L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid order status transition from CANCELLED to CONFIRMED");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateOrderStatus: PENDING -> DELIVERED rejected (skipping fulfillment stages)")
    void shouldRejectSkippingStagesFromPendingToDelivered() {
        order.setOrderStatus(OrderStatus.PENDING);
        AdminOrderStatusUpdateDTO request = new AdminOrderStatusUpdateDTO(
                OrderStatus.DELIVERED, "Skip straight to delivered"
        );

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminOrderService.updateOrderStatus(1L, 500L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid order status transition from PENDING to DELIVERED");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateOrderStatus: Throws ResourceNotFoundException if order does not exist")
    void shouldThrowIfOrderNotFound() {
        AdminOrderStatusUpdateDTO request = new AdminOrderStatusUpdateDTO(OrderStatus.SHIPPED, null);
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminOrderService.updateOrderStatus(1L, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: '999'");
    }

    @Test
    @DisplayName("getOrderById: Accurately calculates gross profit and maps all response fields")
    void shouldCalculateGrossProfitAndMapOrderCorrectly() {
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.findTopByOrderIdOrderByAttemptedAtDesc(500L)).thenReturn(Optional.empty());

        AdminOrderResponseDTO response = adminOrderService.getOrderById(500L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(500L);
        assertThat(response.getCustomerEmail()).isEqualTo("alice@example.com");
        assertThat(response.getCustomerName()).isEqualTo("Alice Smith");
        assertThat(response.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("700.00"));

        // Cost = 200.00 * 2 = 400.00
        // Gross profit = 700.00 - 400.00 = 300.00
        assertThat(response.getGrossProfit()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getUnitCost()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.getItems().get(0).getLineTotal()).isEqualByComparingTo(new BigDecimal("700.00"));
    }
}
