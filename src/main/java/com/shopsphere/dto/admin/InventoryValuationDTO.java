package com.shopsphere.dto.admin;

import java.math.BigDecimal;

public class InventoryValuationDTO {

    private long totalProductsCount;
    private long totalUnitsInStock;
    private BigDecimal totalCostValue = BigDecimal.ZERO;
    private BigDecimal totalRetailValue = BigDecimal.ZERO;
    private BigDecimal potentialGrossProfit = BigDecimal.ZERO;
    private long lowStockProductsCount;

    public InventoryValuationDTO() {
    }

    public long getTotalProductsCount() {
        return totalProductsCount;
    }

    public void setTotalProductsCount(long totalProductsCount) {
        this.totalProductsCount = totalProductsCount;
    }

    public long getTotalUnitsInStock() {
        return totalUnitsInStock;
    }

    public void setTotalUnitsInStock(long totalUnitsInStock) {
        this.totalUnitsInStock = totalUnitsInStock;
    }

    public BigDecimal getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(BigDecimal totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public BigDecimal getTotalRetailValue() {
        return totalRetailValue;
    }

    public void setTotalRetailValue(BigDecimal totalRetailValue) {
        this.totalRetailValue = totalRetailValue;
    }

    public BigDecimal getPotentialGrossProfit() {
        return potentialGrossProfit;
    }

    public void setPotentialGrossProfit(BigDecimal potentialGrossProfit) {
        this.potentialGrossProfit = potentialGrossProfit;
    }

    public long getLowStockProductsCount() {
        return lowStockProductsCount;
    }

    public void setLowStockProductsCount(long lowStockProductsCount) {
        this.lowStockProductsCount = lowStockProductsCount;
    }
}
