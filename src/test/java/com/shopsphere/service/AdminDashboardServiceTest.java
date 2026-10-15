package com.shopsphere.service;

import com.shopsphere.dto.admin.*;
import com.shopsphere.entity.*;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.repository.OrderRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.impl.AdminDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.shopsphere.exception.BadRequestException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    private Category category;
    private Product product1;
    private Product product2;
    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        category = new Category("Electronics", "electronics", "Tech", null);
        category.setId(1L);

        product1 = new Product(
                "SKU-KB-01", "Gaming Keyboard", "gaming-keyboard", "RGB",
                new BigDecimal("50.00"), new BigDecimal("100.00"), 20, 5, null, category
        );
        product1.setId(101L);

        product2 = new Product(
                "SKU-MS-01", "Gaming Mouse", "gaming-mouse", "Optical",
                new BigDecimal("25.00"), new BigDecimal("60.00"), 30, 5, null, category
        );
        product2.setId(102L);

        User customer = new User("cust@example.com", "hash", "Cust One", "+123", Role.ROLE_CUSTOMER);
        customer.setId(2L);

        LocalDate today = LocalDate.now();

        // Order 1: 2 Keyboards ($200 total, $100 cost, $100 profit)
        order1 = new Order(
                "ORD-TEST-001", customer, null,
                new BigDecimal("200.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("200.00")
        );
        order1.setId(1001L);
        order1.setOrderStatus(OrderStatus.CONFIRMED);
        order1.setPaymentStatus(PaymentStatus.PAID);
        order1.setCreatedAt(today.minusDays(1).atTime(10, 0));

        OrderItem item1 = new OrderItem(order1, product1, "Gaming Keyboard", "SKU-KB-01", new BigDecimal("100.00"), 2, new BigDecimal("200.00"));
        item1.setUnitCostSnapshot(new BigDecimal("50.00"));
        order1.setItems(new ArrayList<>(List.of(item1)));

        // Order 2: 1 Keyboard + 1 Mouse ($160 total, $75 cost, $85 profit)
        order2 = new Order(
                "ORD-TEST-002", customer, null,
                new BigDecimal("160.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("160.00")
        );
        order2.setId(1002L);
        order2.setOrderStatus(OrderStatus.DELIVERED);
        order2.setPaymentStatus(PaymentStatus.PAID);
        order2.setCreatedAt(today.atTime(14, 0));

        OrderItem item2a = new OrderItem(order2, product1, "Gaming Keyboard", "SKU-KB-01", new BigDecimal("100.00"), 1, new BigDecimal("100.00"));
        item2a.setUnitCostSnapshot(new BigDecimal("50.00"));

        OrderItem item2b = new OrderItem(order2, product2, "Gaming Mouse", "SKU-MS-01", new BigDecimal("60.00"), 1, new BigDecimal("60.00"));
        item2b.setUnitCostSnapshot(new BigDecimal("25.00"));

        order2.setItems(new ArrayList<>(List.of(item2a, item2b)));
    }

    @Test
    @DisplayName("getDashboardSummary: Correctly calculates revenue, gross profit, and counts excluding cancelled orders")
    void shouldCalculateSummaryMetricsCorrectly() {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        when(orderRepository.findValidOrdersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(order1, order2));
        when(productRepository.count()).thenReturn(25L);
        when(categoryRepository.countByIsActiveTrue()).thenReturn(4L);
        when(productRepository.countLowStockProducts()).thenReturn(3L);
        when(orderRepository.count()).thenReturn(10L);
        when(orderRepository.countByOrderStatusIn(any())).thenReturn(2L);
        when(userRepository.countByRole(Role.ROLE_CUSTOMER)).thenReturn(14L);

        AdminDashboardSummaryDTO summary = adminDashboardService.getDashboardSummary(start, end);

        assertThat(summary).isNotNull();
        // Total revenue: 200 + 160 = 360.00
        assertThat(summary.getPeriodRevenue()).isEqualByComparingTo(new BigDecimal("360.00"));
        // Total cost: (2 * 50) + (1 * 50 + 1 * 25) = 100 + 75 = 175.00
        // Gross profit: 360 - 175 = 185.00
        assertThat(summary.getPeriodProfit()).isEqualByComparingTo(new BigDecimal("185.00"));
        assertThat(summary.getPeriodOrdersCount()).isEqualTo(2L);
        assertThat(summary.getTotalProducts()).isEqualTo(25L);
        assertThat(summary.getTotalCategories()).isEqualTo(4L);
        assertThat(summary.getLowStockCount()).isEqualTo(3L);
        assertThat(summary.getOrdersAwaitingProcessing()).isEqualTo(2L);
        assertThat(summary.getTotalCustomers()).isEqualTo(14L);
    }

    @Test
    @DisplayName("getSalesAnalytics: Generates continuous timeline without gaps for date range")
    void shouldGenerateContinuousDailySalesTimeline() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now();

        when(orderRepository.findValidOrdersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(order1, order2));

        SalesAnalyticsDTO result = adminDashboardService.getSalesAnalytics(start, end);

        assertThat(result).isNotNull();
        assertThat(result.getDailySales()).isNotNull();
        // 3 days inclusive: minusDays(2), minusDays(1), today
        assertThat(result.getDailySales()).hasSize(3);

        // First day has 0 sales, but exists in timeline
        DailySalesDTO day0 = result.getDailySales().get(0);
        assertThat(day0.getDate()).isEqualTo(start);
        assertThat(day0.getRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(day0.getOrderCount()).isEqualTo(0L);

        // Overall totals in the sales analytics DTO
        assertThat(result.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("360.00"));
        assertThat(result.getTotalOrders()).isEqualTo(2L);
        assertThat(result.getTotalProfit()).isEqualByComparingTo(new BigDecimal("185.00"));
        assertThat(result.getAverageOrderValue()).isEqualByComparingTo(new BigDecimal("180.00"));
    }

    @Test
    @DisplayName("getTopSellingProducts: Aggregates units sold and revenue accurately")
    void shouldAggregateTopSellingProducts() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        when(orderRepository.findValidOrdersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(order1, order2));

        List<TopSellingProductDTO> topProducts = adminDashboardService.getTopSellingProducts(5, start, end);

        assertThat(topProducts).hasSize(2);

        // Product 1 (Gaming Keyboard): 2 units (order1) + 1 unit (order2) = 3 units, $300 revenue
        TopSellingProductDTO top1 = topProducts.get(0);
        assertThat(top1.getProductId()).isEqualTo(101L);
        assertThat(top1.getProductName()).isEqualTo("Gaming Keyboard");
        assertThat(top1.getUnitsSold()).isEqualTo(3L);
        assertThat(top1.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("300.00"));

        // Product 2 (Gaming Mouse): 1 unit (order2) = 1 unit, $60 revenue
        TopSellingProductDTO top2 = topProducts.get(1);
        assertThat(top2.getProductId()).isEqualTo(102L);
        assertThat(top2.getUnitsSold()).isEqualTo(1L);
        assertThat(top2.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    @DisplayName("getInventoryValuation: Aggregates warehouse inventory asset valuation")
    void shouldCalculateInventoryValuation() {
        when(productRepository.count()).thenReturn(25L);
        when(productRepository.calculateTotalCostValue()).thenReturn(new BigDecimal("17500.00"));
        when(productRepository.calculateTotalRetailValue()).thenReturn(new BigDecimal("32000.00"));
        when(productRepository.calculateTotalUnitsInStock()).thenReturn(450L);
        when(productRepository.countLowStockProducts()).thenReturn(4L);

        InventoryValuationDTO valuation = adminDashboardService.getInventoryValuation();

        assertThat(valuation).isNotNull();
        assertThat(valuation.getTotalCostValue()).isEqualByComparingTo(new BigDecimal("17500.00"));
        assertThat(valuation.getTotalRetailValue()).isEqualByComparingTo(new BigDecimal("32000.00"));
        // Unrealized gross profit: 32000.00 - 17500.00 = 14500.00
        assertThat(valuation.getPotentialGrossProfit()).isEqualByComparingTo(new BigDecimal("14500.00"));
        assertThat(valuation.getTotalUnitsInStock()).isEqualTo(450L);
        assertThat(valuation.getLowStockProductsCount()).isEqualTo(4L);
        assertThat(valuation.getTotalProductsCount()).isEqualTo(25L);
    }

    @Test
    @DisplayName("resolveDateRange: Throws BadRequestException when startDate is after endDate")
    void shouldThrowBadRequestExceptionWhenStartDateAfterEndDate() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now();

        assertThatThrownBy(() -> adminDashboardService.getDashboardSummary(start, end))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be after end date");
    }

    @Test
    @DisplayName("resolveDateRange: Throws BadRequestException when future startDate with null endDate")
    void shouldThrowBadRequestExceptionWhenFutureStartDateWithNullEndDate() {
        LocalDate futureStart = LocalDate.now().plusDays(10);

        assertThatThrownBy(() -> adminDashboardService.getSalesAnalytics(futureStart, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be after end date");
    }

    @Test
    @DisplayName("Zero orders: Gracefully returns zero totals without DivisionByZero or NullPointerException")
    void shouldHandleZeroOrdersGracefully() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();

        when(orderRepository.findValidOrdersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(productRepository.count()).thenReturn(10L);
        when(categoryRepository.countByIsActiveTrue()).thenReturn(2L);
        when(productRepository.countLowStockProducts()).thenReturn(0L);
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.countByOrderStatusIn(any())).thenReturn(0L);
        when(userRepository.countByRole(Role.ROLE_CUSTOMER)).thenReturn(5L);

        AdminDashboardSummaryDTO summary = adminDashboardService.getDashboardSummary(start, end);
        assertThat(summary.getPeriodOrdersCount()).isEqualTo(0L);
        assertThat(summary.getPeriodRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getPeriodProfit()).isEqualByComparingTo(BigDecimal.ZERO);

        SalesAnalyticsDTO sales = adminDashboardService.getSalesAnalytics(start, end);
        assertThat(sales.getTotalOrders()).isEqualTo(0L);
        assertThat(sales.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sales.getTotalProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sales.getAverageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sales.getDailySales()).hasSize(6); // 5 days ago to today inclusive

        List<TopSellingProductDTO> top = adminDashboardService.getTopSellingProducts(5, start, end);
        assertThat(top).isEmpty();

        List<CategorySalesDTO> catSales = adminDashboardService.getCategorySales(start, end);
        assertThat(catSales).isEmpty();
    }
}
