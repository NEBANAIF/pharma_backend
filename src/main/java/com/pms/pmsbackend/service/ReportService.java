package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.*;
import com.pms.pmsbackend.repository.BatchRepository;
import com.pms.pmsbackend.repository.ExpenseRepository;
import com.pms.pmsbackend.repository.SaleItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleItemRepository saleItemRepository;
    private final ExpenseRepository expenseRepository;
    private final MedicineService medicineService;
    private final BatchRepository batchRepository;

    public ProfitLossResponse getProfitAndLoss(LocalDate from, LocalDate to) {
        ProfitLossResponse response = new ProfitLossResponse();
        response.setFrom(from);
        response.setTo(to);

        BigDecimal revenue = saleItemRepository.sumRevenueForRange(from, to);
        BigDecimal cost = saleItemRepository.sumCostForRange(from, to);
        BigDecimal expenses = expenseRepository.sumForRange(from, to);

        response.setRevenue(revenue);
        response.setCostOfGoodsSold(cost);
        response.setGrossProfit(revenue.subtract(cost));
        response.setTotalExpenses(expenses);
        response.setNetProfit(revenue.subtract(cost).subtract(expenses));

        return response;
    }

    public List<BestSellingItemDto> getBestSelling(LocalDate from, LocalDate to, int limit) {
        return saleItemRepository.findBestSellingForRange(from, to).stream()
                .map(row -> {
                    BestSellingItemDto dto = new BestSellingItemDto();
                    dto.setMedicineId((Integer) row[0]);
                    dto.setMedicineName((String) row[1]);
                    dto.setQuantitySold((Long) row[2]);
                    dto.setRevenue((BigDecimal) row[3]);
                    return dto;
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    public InventoryValuationResponse getInventoryValuation() {
        List<MedicineDto> medicines = medicineService.findAll();

        InventoryValuationResponse response = new InventoryValuationResponse();
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        List<InventoryValuationResponse.Item> items = new java.util.ArrayList<>();

        for (MedicineDto m : medicines) {
            int stock = m.getCurrentStock() != null ? m.getCurrentStock() : 0;
            if (stock == 0) continue;

            BigDecimal costValue = m.getPurchasePrice().multiply(BigDecimal.valueOf(stock));
            BigDecimal retailValue = m.getSellingPrice().multiply(BigDecimal.valueOf(stock));

            InventoryValuationResponse.Item item = new InventoryValuationResponse.Item();
            item.setMedicineId(m.getId());
            item.setMedicineName(m.getName());
            item.setStock(stock);
            item.setCostValue(costValue);
            item.setRetailValue(retailValue);
            items.add(item);

            totalCost = totalCost.add(costValue);
            totalRetail = totalRetail.add(retailValue);
        }

        items.sort((a, b) -> b.getCostValue().compareTo(a.getCostValue()));

        response.setItems(items);
        response.setTotalCostValue(totalCost);
        response.setTotalRetailValue(totalRetail);
        return response;
    }

    public InventoryValuationByBranchResponse getInventoryValuationByBranch() {
        Map<Integer, MedicineDto> medicineById = medicineService.findAll().stream()
                .collect(Collectors.toMap(MedicineDto::getId, m -> m));

        Map<Integer, InventoryValuationByBranchResponse.BranchValuation> byBranch = new LinkedHashMap<>();

        for (BatchRepository.BranchMedicineStock row : batchRepository.sumQuantityGroupedByBranchAndMedicine()) {
            int stock = row.getQty() != null ? row.getQty() : 0;
            if (stock == 0) continue;

            MedicineDto medicine = medicineById.get(row.getMedicineId());
            if (medicine == null) continue; // stale batch pointing at a deleted medicine

            InventoryValuationByBranchResponse.BranchValuation branchValuation = byBranch.computeIfAbsent(row.getBranchId(), id -> {
                InventoryValuationByBranchResponse.BranchValuation bv = new InventoryValuationByBranchResponse.BranchValuation();
                bv.setBranchId(id);
                bv.setBranchName(row.getBranchName());
                bv.setTotalCostValue(BigDecimal.ZERO);
                bv.setTotalRetailValue(BigDecimal.ZERO);
                bv.setItems(new ArrayList<>());
                return bv;
            });

            BigDecimal costValue = medicine.getPurchasePrice().multiply(BigDecimal.valueOf(stock));
            BigDecimal retailValue = medicine.getSellingPrice().multiply(BigDecimal.valueOf(stock));

            InventoryValuationResponse.Item item = new InventoryValuationResponse.Item();
            item.setMedicineId(medicine.getId());
            item.setMedicineName(medicine.getName());
            item.setStock(stock);
            item.setCostValue(costValue);
            item.setRetailValue(retailValue);
            branchValuation.getItems().add(item);

            branchValuation.setTotalCostValue(branchValuation.getTotalCostValue().add(costValue));
            branchValuation.setTotalRetailValue(branchValuation.getTotalRetailValue().add(retailValue));
        }

        List<InventoryValuationByBranchResponse.BranchValuation> branches = new ArrayList<>(byBranch.values());
        branches.forEach(bv -> bv.getItems().sort((a, b) -> b.getCostValue().compareTo(a.getCostValue())));
        branches.sort(Comparator.comparing(InventoryValuationByBranchResponse.BranchValuation::getBranchName));

        InventoryValuationByBranchResponse response = new InventoryValuationByBranchResponse();
        response.setBranches(branches);
        return response;
    }
}
