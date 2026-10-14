package com.shopsphere.controller;

import com.shopsphere.dto.admin.*;
import com.shopsphere.dto.common.ApiResponse;
import com.shopsphere.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Dashboard & Analytics", description = "Endpoints for executive KPIs, sales charts, and business reports")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard overview KPIs", description = "Retrieves high-level counts, revenue, and profit for the selected period")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryDTO>> getDashboardSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AdminDashboardSummaryDTO summary = dashboardService.getDashboardSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping({"/dashboard/sales", "/reports/sales"})
    @Operation(summary = "Get sales analytics & daily time-series", description = "Retrieves daily revenue, profit, and order volume for charting")
    public ResponseEntity<ApiResponse<SalesAnalyticsDTO>> getSalesAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        SalesAnalyticsDTO analytics = dashboardService.getSalesAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping({"/dashboard/top-products", "/reports/top-products"})
    @Operation(summary = "Get top-selling products", description = "Retrieves best-performing products by units sold, revenue, and profit")
    public ResponseEntity<ApiResponse<List<TopSellingProductDTO>>> getTopSellingProducts(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TopSellingProductDTO> topProducts = dashboardService.getTopSellingProducts(limit, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(topProducts));
    }

    @GetMapping("/reports/categories")
    @Operation(summary = "Get sales by category", description = "Retrieves category-level revenue and profit distribution")
    public ResponseEntity<ApiResponse<List<CategorySalesDTO>>> getCategorySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CategorySalesDTO> categorySales = dashboardService.getCategorySales(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(categorySales));
    }

    @GetMapping("/reports/inventory")
    @Operation(summary = "Get warehouse inventory valuation", description = "Calculates total cost assets, retail valuation, and potential gross profit")
    public ResponseEntity<ApiResponse<InventoryValuationDTO>> getInventoryValuation() {
        InventoryValuationDTO valuation = dashboardService.getInventoryValuation();
        return ResponseEntity.ok(ApiResponse.success(valuation));
    }
}
