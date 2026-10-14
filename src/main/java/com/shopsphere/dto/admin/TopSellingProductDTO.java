package com.shopsphere.dto.admin;

import java.math.BigDecimal;

public class TopSellingProductDTO {

    private Long productId;
    private String productName;
    private String productSku;
    private String categoryName;
    private long unitsSold;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal totalProfit = BigDecimal.ZERO;

    public TopSellingProductDTO() {
    }

    public TopSellingProductDTO(Long productId, String productName, String productSku,
                                String categoryName, long unitsSold, BigDecimal totalRevenue, BigDecimal totalProfit) {
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.categoryName = categoryName;
        this.unitsSold = unitsSold;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        this.totalProfit = totalProfit != null ? totalProfit : BigDecimal.ZERO;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
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

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(BigDecimal totalProfit) {
        this.totalProfit = totalProfit;
    }
}
