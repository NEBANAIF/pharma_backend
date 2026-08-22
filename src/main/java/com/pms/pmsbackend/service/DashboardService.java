package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.DashboardResponse;
import com.pms.pmsbackend.dto.InventoryValuationResponse;
import com.pms.pmsbackend.dto.MedicineDto;
import com.pms.pmsbackend.dto.SaleResponse;
import com.pms.pmsbackend.entity.Batch;
import com.pms.pmsbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final BatchRepository batchRepository;
    private final MedicineService medicineService;
    private final SaleService saleService;
    private final ReportService reportService;
    private final SettingsService settingsService;

    private static final int EXPIRY_WARNING_DAYS = 30;
    private static final int EXPIRY_CRITICAL_DAYS = 7;
    private static final int PURCHASE_OVERDUE_DAYS = 7;
    private static final int CATEGORY_MIX_TOP_N = 4; // + one "Other" bucket for the rest

    public DashboardResponse getDashboard() {
        DashboardResponse response = new DashboardResponse();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        BigDecimal salesToday = saleRepository.sumTotalForToday();
        BigDecimal salesYesterday = saleRepository.sumTotalForDate(yesterday);
        BigDecimal profitToday = saleItemRepository.sumProfitForToday();
        BigDecimal profitYesterday = saleItemRepository.sumProfitForDate(yesterday);

        response.setTotalSalesToday(salesToday);
        response.setProfitToday(profitToday);
        response.setSalesDeltaPercent(percentChange(salesYesterday, salesToday));
        response.setProfitDeltaPercent(percentChange(profitYesterday, profitToday));

        response.setTotalCustomers(customerRepository.count());
        response.setTotalSuppliers(supplierRepository.count());
        response.setPendingPurchaseOrders(purchaseRepository.countByStatus("PENDING"));
        response.setOverduePurchaseOrders(
                purchaseRepository.countByStatusAndPurchaseDateBefore("PENDING", today.minusDays(PURCHASE_OVERDUE_DAYS)));

        List<MedicineDto> allMedicines = medicineService.findAll();

        List<DashboardResponse.LowStockMedicineDto> lowStock = allMedicines.stream()
                .filter(m -> m.getCurrentStock() != null && m.getCurrentStock() > 0
                        && m.getCurrentStock() <= (m.getReorderLevel() != null ? m.getReorderLevel() : 10))
                .map(m -> {
                    DashboardResponse.LowStockMedicineDto dto = new DashboardResponse.LowStockMedicineDto();
                    dto.setId(m.getId());
                    dto.setName(m.getName());
                    dto.setCurrentStock(m.getCurrentStock());
                    dto.setReorderLevel(m.getReorderLevel());
                    return dto;
                })
                .collect(Collectors.toList());

        long outOfStockCount = allMedicines.stream()
                .filter(m -> m.getCurrentStock() == null || m.getCurrentStock() == 0)
                .count();

        response.setLowStockMedicines(lowStock);
        response.setLowStockCount(lowStock.size());
        response.setOutOfStockCount(outOfStockCount);

        // Stock health: share of active, reorder-tracked medicines currently
        // stocked *above* their reorder level (i.e. not low, not out).
        List<MedicineDto> tracked = allMedicines.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .collect(Collectors.toList());
        long aboveReorder = tracked.stream()
                .filter(m -> m.getCurrentStock() != null
                        && m.getCurrentStock() > (m.getReorderLevel() != null ? m.getReorderLevel() : 10))
                .count();
        response.setTotalTrackedMedicines(tracked.size());
        response.setMedicinesAboveReorder(aboveReorder);
        response.setStockHealthPercent(tracked.isEmpty() ? null : (aboveReorder * 100.0) / tracked.size());

        // Category mix: share of total on-hand stock quantity per category,
        // top N by volume plus an "Other" bucket for the rest.
        response.setCategoryMix(buildCategoryMix(allMedicines));

        LocalDate cutoff = today.plusDays(EXPIRY_WARNING_DAYS);
        List<Batch> expiring = batchRepository.findExpiringBySoonCutoff(cutoff);

        List<DashboardResponse.ExpiringBatchDto> expiringDtos = expiring.stream()
                .map(b -> {
                    DashboardResponse.ExpiringBatchDto dto = new DashboardResponse.ExpiringBatchDto();
                    dto.setBatchId(b.getId());
                    dto.setMedicineName(b.getMedicine().getName());
                    dto.setBatchNumber(b.getBatchNumber());
                    dto.setExpiryDate(b.getExpiryDate());
                    dto.setQuantity(b.getQuantity());
                    dto.setDaysLeft(ChronoUnit.DAYS.between(today, b.getExpiryDate()));
                    return dto;
                })
                .collect(Collectors.toList());

        response.setExpiringBatches(expiringDtos);
        response.setExpiringSoonCount(expiringDtos.size());
        response.setExpiringCriticalCount(expiringDtos.stream().filter(d -> d.getDaysLeft() < EXPIRY_CRITICAL_DAYS).count());
        response.setExpiringWarningCount(expiringDtos.stream().filter(d -> d.getDaysLeft() >= EXPIRY_CRITICAL_DAYS).count());

        List<SaleResponse> recentSales = saleService.findAll().stream()
                .limit(5)
                .collect(Collectors.toList());
        response.setRecentSales(recentSales);

        response.setSalesLast7Days(mapTrendRows(saleRepository.sumTotalsByDayLast7Days()));
        response.setProfitLast7Days(mapTrendRows(saleItemRepository.sumProfitByDayLast7Days()));

        response.setBranchPerformance(buildBranchPerformance(today));

        // Reuse the Inventory Valuation report's own calculation (cost price
        // * on-hand quantity, summed) instead of recomputing it here, so the
        // dashboard figure can never drift out of sync with the Reports page.
        InventoryValuationResponse valuation = reportService.getInventoryValuation();
        response.setTotalInventoryValue(valuation.getTotalCostValue());
        response.setTotalInventoryRetailValue(valuation.getTotalRetailValue());

        response.setTopSellingMedicines(mapTopSellers(saleItemRepository.findTopSellersLast30Days(5)));
        response.setLeastSellingMedicines(mapTopSellers(saleItemRepository.findLeastSellersLast30Days(5)));

        response.setCurrencySymbol(settingsService.getOrCreate().getCurrencySymbol());

        return response;
    }

    private List<DashboardResponse.TopSellerDto> mapTopSellers(List<Object[]> rows) {
        return rows.stream()
                .map(row -> {
                    DashboardResponse.TopSellerDto dto = new DashboardResponse.TopSellerDto();
                    dto.setId((Integer) row[0]);
                    dto.setName((String) row[1]);
                    dto.setQuantitySold(((Number) row[2]).longValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // GROUP BY in the underlying query means a day with zero sales/profit is
    // simply absent from `rows`, not present with a total of 0. Zero-fill the
    // full 7-day window here so the sparkline always renders 7 bars and the
    // "highlight the last bar" logic on the frontend actually highlights
    // today, not just whatever day happened to have a nonzero total last.
    private List<DashboardResponse.DailySalesPointDto> mapTrendRows(List<Object[]> rows) {
        Map<LocalDate, BigDecimal> totalsByDate = rows.stream()
                .collect(Collectors.toMap(
                        row -> ((java.sql.Date) row[0]).toLocalDate(),
                        row -> (BigDecimal) row[1]));

        LocalDate today = LocalDate.now();
        List<DashboardResponse.DailySalesPointDto> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            DashboardResponse.DailySalesPointDto dto = new DashboardResponse.DailySalesPointDto();
            dto.setDate(date);
            dto.setTotal(totalsByDate.getOrDefault(date, BigDecimal.ZERO));
            result.add(dto);
        }
        return result;
    }

    /** Null when there's no prior-period figure to compare against (avoids implying "flat" with a false 0%). */
    private Double percentChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || current == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private List<DashboardResponse.CategoryMixDto> buildCategoryMix(List<MedicineDto> allMedicines) {
        Map<String, Long> quantityByCategory = new LinkedHashMap<>();
        long total = 0;
        for (MedicineDto m : allMedicines) {
            if (m.getCurrentStock() == null || m.getCurrentStock() <= 0) continue;
            String category = m.getCategoryName() != null ? m.getCategoryName() : "Uncategorized";
            quantityByCategory.merge(category, (long) m.getCurrentStock(), Long::sum);
            total += m.getCurrentStock();
        }

        if (total == 0) return List.of();

        List<Map.Entry<String, Long>> sorted = quantityByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<DashboardResponse.CategoryMixDto> result = new ArrayList<>();
        long otherQuantity = 0;
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Long> entry = sorted.get(i);
            if (i < CATEGORY_MIX_TOP_N) {
                DashboardResponse.CategoryMixDto dto = new DashboardResponse.CategoryMixDto();
                dto.setCategoryName(entry.getKey());
                dto.setStockQuantity(entry.getValue());
                dto.setPercent(round1((entry.getValue() * 100.0) / total));
                result.add(dto);
            } else {
                otherQuantity += entry.getValue();
            }
        }
        if (otherQuantity > 0) {
            DashboardResponse.CategoryMixDto other = new DashboardResponse.CategoryMixDto();
            other.setCategoryName("Other");
            other.setStockQuantity(otherQuantity);
            other.setPercent(round1((otherQuantity * 100.0) / total));
            result.add(other);
        }
        return result;
    }

    private List<DashboardResponse.BranchPerformanceDto> buildBranchPerformance(LocalDate today) {
        LocalDate monthStart = today.withDayOfMonth(1);
        List<Object[]> rows = saleRepository.sumTotalsByBranchForRange(monthStart, today);

        BigDecimal companyTotal = rows.stream()
                .map(row -> (BigDecimal) row[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DashboardResponse.BranchPerformanceDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            DashboardResponse.BranchPerformanceDto dto = new DashboardResponse.BranchPerformanceDto();
            dto.setBranchId((Integer) row[0]);
            dto.setBranchName((String) row[1]);
            BigDecimal branchTotal = (BigDecimal) row[2];
            dto.setSalesThisMonth(branchTotal);
            dto.setPercentOfCompanyTotal(
                    companyTotal.compareTo(BigDecimal.ZERO) == 0
                            ? 0
                            : round1(branchTotal.doubleValue() * 100.0 / companyTotal.doubleValue()));
            result.add(dto);
        }
        result.sort(Comparator.comparingDouble(DashboardResponse.BranchPerformanceDto::getPercentOfCompanyTotal).reversed());
        return result;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}