package com.shopsphere.dto.admin;

import java.math.BigDecimal;

public class CategorySalesDTO {

    private Long categoryId;
    private String categoryName;
    private long unitsSold;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal profit = BigDecimal.ZERO;

    public CategorySalesDTO() {
    }

    public CategorySalesDTO(Long categoryId, String categoryName, long unitsSold,
                            BigDecimal revenue, BigDecimal profit) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.unitsSold = unitsSold;
        this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
        this.profit = profit != null ? profit : BigDecimal.ZERO;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public long getUnitsSold() {
        return unitsSold;
    }

    public void setUnitsSold(long unitsSold) {
        this.unitsSold = unitsSold;
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
