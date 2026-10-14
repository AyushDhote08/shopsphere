package com.shopsphere.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AdminDashboardSummaryDTO {

    private long totalProducts;
    private long totalCategories;
    private long totalCustomers;
    private long totalOrders;
    private long ordersAwaitingProcessing;
    private long lowStockCount;

    private BigDecimal periodRevenue = BigDecimal.ZERO;
    private BigDecimal periodProfit = BigDecimal.ZERO;
    private long periodOrdersCount;

    private LocalDate periodStartDate;
    private LocalDate periodEndDate;

    public AdminDashboardSummaryDTO() {
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalCategories() {
        return totalCategories;
    }

    public void setTotalCategories(long totalCategories) {
        this.totalCategories = totalCategories;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getOrdersAwaitingProcessing() {
        return ordersAwaitingProcessing;
    }

    public void setOrdersAwaitingProcessing(long ordersAwaitingProcessing) {
        this.ordersAwaitingProcessing = ordersAwaitingProcessing;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public BigDecimal getPeriodRevenue() {
        return periodRevenue;
    }

    public void setPeriodRevenue(BigDecimal periodRevenue) {
        this.periodRevenue = periodRevenue;
    }

    public BigDecimal getPeriodProfit() {
        return periodProfit;
    }

    public void setPeriodProfit(BigDecimal periodProfit) {
        this.periodProfit = periodProfit;
    }

    public long getPeriodOrdersCount() {
        return periodOrdersCount;
    }

    public void setPeriodOrdersCount(long periodOrdersCount) {
        this.periodOrdersCount = periodOrdersCount;
    }

    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

    public void setPeriodStartDate(LocalDate periodStartDate) {
        this.periodStartDate = periodStartDate;
    }

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public void setPeriodEndDate(LocalDate periodEndDate) {
        this.periodEndDate = periodEndDate;
    }
}
