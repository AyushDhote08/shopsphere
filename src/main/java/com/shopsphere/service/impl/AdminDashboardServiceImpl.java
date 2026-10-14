package com.shopsphere.service.impl;

import com.shopsphere.dto.admin.*;
import com.shopsphere.entity.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.repository.*;
import com.shopsphere.service.AdminDashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AdminDashboardServiceImpl(ProductRepository productRepository,
                                     CategoryRepository categoryRepository,
                                     OrderRepository orderRepository,
                                     UserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AdminDashboardSummaryDTO getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        LocalDate start = range[0];
        LocalDate end = range[1];

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();

        AdminDashboardSummaryDTO summary = new AdminDashboardSummaryDTO();
        summary.setTotalProducts(productRepository.count());
        summary.setTotalCategories(categoryRepository.countByIsActiveTrue());
        summary.setTotalCustomers(userRepository.countByRole(Role.ROLE_CUSTOMER));
        summary.setTotalOrders(orderRepository.count());
        summary.setOrdersAwaitingProcessing(orderRepository.countByOrderStatusIn(
                List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED)
        ));
        summary.setLowStockCount(productRepository.countLowStockProducts());

        summary.setPeriodStartDate(start);
        summary.setPeriodEndDate(end);

        List<Order> validOrders = orderRepository.findValidOrdersBetween(startDateTime, endDateTime);
        summary.setPeriodOrdersCount(validOrders.size());

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;

        for (Order o : validOrders) {
            revenue = revenue.add(o.getTotalAmount());
            profit = profit.add(calculateOrderProfit(o));
        }

        summary.setPeriodRevenue(revenue.setScale(2, RoundingMode.HALF_UP));
        summary.setPeriodProfit(profit.setScale(2, RoundingMode.HALF_UP));

        return summary;
    }

    @Override
    public SalesAnalyticsDTO getSalesAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        LocalDate start = range[0];
        LocalDate end = range[1];

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();

        List<Order> validOrders = orderRepository.findValidOrdersBetween(startDateTime, endDateTime);

        // Pre-populate continuous timeline so charts don't have gaps
        Map<LocalDate, DailySalesDTO> dailyMap = new LinkedHashMap<>();
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            dailyMap.put(cur, new DailySalesDTO(cur, 0, BigDecimal.ZERO, BigDecimal.ZERO));
            cur = cur.plusDays(1);
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        for (Order o : validOrders) {
            LocalDate orderDate = o.getCreatedAt().toLocalDate();
            DailySalesDTO daily = dailyMap.get(orderDate);

            if (daily != null) {
                daily.setOrderCount(daily.getOrderCount() + 1);
                daily.setRevenue(daily.getRevenue().add(o.getTotalAmount()));
                BigDecimal orderProfit = calculateOrderProfit(o);
                daily.setProfit(daily.getProfit().add(orderProfit));

                totalRevenue = totalRevenue.add(o.getTotalAmount());
                totalProfit = totalProfit.add(orderProfit);
            }
        }

        SalesAnalyticsDTO dto = new SalesAnalyticsDTO();
        dto.setStartDate(start);
        dto.setEndDate(end);
        dto.setTotalOrders(validOrders.size());
        dto.setTotalRevenue(totalRevenue.setScale(2, RoundingMode.HALF_UP));
        dto.setTotalProfit(totalProfit.setScale(2, RoundingMode.HALF_UP));

        if (!validOrders.isEmpty()) {
            dto.setAverageOrderValue(totalRevenue.divide(BigDecimal.valueOf(validOrders.size()), 2, RoundingMode.HALF_UP));
        } else {
            dto.setAverageOrderValue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        dto.setDailySales(new ArrayList<>(dailyMap.values()));
        return dto;
    }

    @Override
    public List<TopSellingProductDTO> getTopSellingProducts(int limit, LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        LocalDateTime startDateTime = range[0].atStartOfDay();
        LocalDateTime endDateTime = range[1].plusDays(1).atStartOfDay();

        List<Order> validOrders = orderRepository.findValidOrdersBetween(startDateTime, endDateTime);

        Map<String, TopSellingProductDTO> productMap = new HashMap<>();

        for (Order o : validOrders) {
            for (OrderItem it : o.getItems()) {
                String sku = it.getProductSkuSnapshot();
                TopSellingProductDTO existing = productMap.computeIfAbsent(sku, k -> {
                    TopSellingProductDTO p = new TopSellingProductDTO();
                    p.setProductSku(sku);
                    p.setProductName(it.getProductNameSnapshot());
                    if (it.getProduct() != null) {
                        p.setProductId(it.getProduct().getId());
                        if (it.getProduct().getCategory() != null) {
                            p.setCategoryName(it.getProduct().getCategory().getName());
                        }
                    }
                    if (p.getCategoryName() == null) {
                        p.setCategoryName("Uncategorized");
                    }
                    return p;
                });

                existing.setUnitsSold(existing.getUnitsSold() + it.getQuantity());
                existing.setTotalRevenue(existing.getTotalRevenue().add(it.getLineTotal()));

                BigDecimal unitCost = it.getUnitCostSnapshot() != null ? it.getUnitCostSnapshot() : BigDecimal.ZERO;
                BigDecimal lineCost = unitCost.multiply(BigDecimal.valueOf(it.getQuantity()));
                BigDecimal profit = it.getLineTotal().subtract(lineCost);
                existing.setTotalProfit(existing.getTotalProfit().add(profit));
            }
        }

        return productMap.values().stream()
                .sorted((a, b) -> Long.compare(b.getUnitsSold(), a.getUnitsSold()))
                .limit(limit > 0 ? limit : 5)
                .peek(p -> {
                    p.setTotalRevenue(p.getTotalRevenue().setScale(2, RoundingMode.HALF_UP));
                    p.setTotalProfit(p.getTotalProfit().setScale(2, RoundingMode.HALF_UP));
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CategorySalesDTO> getCategorySales(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        LocalDateTime startDateTime = range[0].atStartOfDay();
        LocalDateTime endDateTime = range[1].plusDays(1).atStartOfDay();

        List<Order> validOrders = orderRepository.findValidOrdersBetween(startDateTime, endDateTime);

        Map<Long, CategorySalesDTO> categoryMap = new HashMap<>();

        for (Order o : validOrders) {
            for (OrderItem it : o.getItems()) {
                Product product = it.getProduct();
                Long catId = 0L;
                String name = "Uncategorized";

                if (product != null && product.getCategory() != null) {
                    catId = product.getCategory().getId();
                    name = product.getCategory().getName();
                }

                final String catName = name;
                CategorySalesDTO catDTO = categoryMap.computeIfAbsent(catId, id -> new CategorySalesDTO(id, catName, 0, BigDecimal.ZERO, BigDecimal.ZERO));

                catDTO.setUnitsSold(catDTO.getUnitsSold() + it.getQuantity());
                catDTO.setRevenue(catDTO.getRevenue().add(it.getLineTotal()));

                BigDecimal unitCost = it.getUnitCostSnapshot() != null ? it.getUnitCostSnapshot() : BigDecimal.ZERO;
                BigDecimal lineCost = unitCost.multiply(BigDecimal.valueOf(it.getQuantity()));
                BigDecimal profit = it.getLineTotal().subtract(lineCost);
                catDTO.setProfit(catDTO.getProfit().add(profit));
            }
        }

        return categoryMap.values().stream()
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .peek(c -> {
                    c.setRevenue(c.getRevenue().setScale(2, RoundingMode.HALF_UP));
                    c.setProfit(c.getProfit().setScale(2, RoundingMode.HALF_UP));
                })
                .collect(Collectors.toList());
    }

    @Override
    public InventoryValuationDTO getInventoryValuation() {
        InventoryValuationDTO dto = new InventoryValuationDTO();
        dto.setTotalProductsCount(productRepository.count());
        dto.setTotalUnitsInStock(productRepository.calculateTotalUnitsInStock());
        dto.setLowStockProductsCount(productRepository.countLowStockProducts());

        BigDecimal costValue = productRepository.calculateTotalCostValue().setScale(2, RoundingMode.HALF_UP);
        BigDecimal retailValue = productRepository.calculateTotalRetailValue().setScale(2, RoundingMode.HALF_UP);
        BigDecimal potentialProfit = retailValue.subtract(costValue).setScale(2, RoundingMode.HALF_UP);

        dto.setTotalCostValue(costValue);
        dto.setTotalRetailValue(retailValue);
        dto.setPotentialGrossProfit(potentialProfit);
        return dto;
    }

    private BigDecimal calculateOrderProfit(Order order) {
        BigDecimal totalCost = BigDecimal.ZERO;
        for (OrderItem it : order.getItems()) {
            BigDecimal unitCost = it.getUnitCostSnapshot() != null ? it.getUnitCostSnapshot() : BigDecimal.ZERO;
            BigDecimal lineCost = unitCost.multiply(BigDecimal.valueOf(it.getQuantity()));
            totalCost = totalCost.add(lineCost);
        }
        return order.getSubtotalAmount().subtract(totalCost);
    }

    private LocalDate[] resolveDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Start date (" + start + ") cannot be after end date (" + end + ")");
        }
        LocalDate resolvedEnd = (end != null) ? end : LocalDate.now();
        LocalDate resolvedStart = (start != null) ? start : resolvedEnd.minusDays(29);
        return new LocalDate[]{resolvedStart, resolvedEnd};
    }
}
