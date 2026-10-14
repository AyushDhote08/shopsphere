package com.shopsphere.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailySalesDTO {

    private LocalDate date;
    private long orderCount;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal profit = BigDecimal.ZERO;

    public DailySalesDTO() {
    }

    public DailySalesDTO(LocalDate date, long orderCount, BigDecimal revenue, BigDecimal profit) {
        this.date = date;
        this.orderCount = orderCount;
        this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
        this.profit = profit != null ? profit : BigDecimal.ZERO;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }
}
