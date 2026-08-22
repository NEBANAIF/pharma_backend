package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DashboardResponse {

    private BigDecimal totalSalesToday;
    private BigDecimal profitToday;
    private long totalCustomers;
    private long totalSuppliers;
    private long pendingPurchaseOrders;
    private long overduePurchaseOrders;
    private long lowStockCount;
    private long outOfStockCount;
    private long expiringSoonCount;

    // % change vs yesterday. Null (not zero) when yesterday had no sales/profit
    // to compare against -- showing "0%" there would misleadingly imply flat
    // performance rather than "no baseline yet".
    private Double salesDeltaPercent;
    private Double profitDeltaPercent;

    // Share of tracked medicines currently stocked above their reorder level.
    private Double stockHealthPercent;
    private long medicinesAboveReorder;
    private long totalTrackedMedicines;

    private long expiringCriticalCount; // < 7 days left
    private long expiringWarningCount;  // 7-30 days left

    // Sum of purchasePrice * currentStock across all medicines -- reuses the
    // exact same calculation as the Inventory Valuation report so the two
    // numbers never drift apart.
    private BigDecimal totalInventoryValue;
    private BigDecimal totalInventoryRetailValue;

    private List<TopSellerDto> topSellingMedicines;
    private List<TopSellerDto> leastSellingMedicines;

    // From Settings, so every money figure on the dashboard formats using
    // whatever currency the pharmacy has actually configured (defaults to
    // "$" / USD) instead of a hardcoded symbol.
    private String currencySymbol;

    private List<SaleResponse> recentSales;
    private List<LowStockMedicineDto> lowStockMedicines;
    private List<ExpiringBatchDto> expiringBatches;
    private List<DailySalesPointDto> salesLast7Days;
    private List<DailySalesPointDto> profitLast7Days;
    private List<CategoryMixDto> categoryMix;
    private List<BranchPerformanceDto> branchPerformance;

    @Getter
    @Setter
    public static class LowStockMedicineDto {
        private Integer id;
        private String name;
        private Integer currentStock;
        private Integer reorderLevel;
    }

    @Getter
    @Setter
    public static class ExpiringBatchDto {
        private Integer batchId;
        private String medicineName;
        private String batchNumber;
        private LocalDate expiryDate;
        private Integer quantity;
        private long daysLeft;
    }

    @Getter
    @Setter
    public static class DailySalesPointDto {
        private LocalDate date;
        private BigDecimal total;
    }

    @Getter
    @Setter
    public static class CategoryMixDto {
        private String categoryName;
        private long stockQuantity;
        private double percent;
    }

    @Getter
    @Setter
    public static class TopSellerDto {
        private Integer id;
        private String name;
        private long quantitySold;
    }

    @Getter
    @Setter
    public static class BranchPerformanceDto {
        private Integer branchId;
        private String branchName;
        private BigDecimal salesThisMonth;
        private double percentOfCompanyTotal;
    }
}