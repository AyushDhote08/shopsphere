package com.shopsphere.service;

import com.shopsphere.dto.admin.*;

import java.time.LocalDate;
import java.util.List;

public interface AdminDashboardService {

    AdminDashboardSummaryDTO getDashboardSummary(LocalDate startDate, LocalDate endDate);

    SalesAnalyticsDTO getSalesAnalytics(LocalDate startDate, LocalDate endDate);

    List<TopSellingProductDTO> getTopSellingProducts(int limit, LocalDate startDate, LocalDate endDate);

    List<CategorySalesDTO> getCategorySales(LocalDate startDate, LocalDate endDate);

    InventoryValuationDTO getInventoryValuation();
}
