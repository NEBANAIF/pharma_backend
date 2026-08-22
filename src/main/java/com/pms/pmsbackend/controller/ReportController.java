package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.BestSellingItemDto;
import com.pms.pmsbackend.dto.InventoryValuationByBranchResponse;
import com.pms.pmsbackend.dto.InventoryValuationResponse;
import com.pms.pmsbackend.dto.ProfitLossResponse;
import com.pms.pmsbackend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/profit-loss")
    public ProfitLossResponse getProfitAndLoss(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.getProfitAndLoss(from, to);
    }

    @GetMapping("/best-selling")
    public List<BestSellingItemDto> getBestSelling(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return reportService.getBestSelling(from, to, limit);
    }

    @GetMapping("/inventory-valuation")
    public InventoryValuationResponse getInventoryValuation() {
        return reportService.getInventoryValuation();
    }

    @GetMapping("/inventory-valuation/by-branch")
    public InventoryValuationByBranchResponse getInventoryValuationByBranch() {
        return reportService.getInventoryValuationByBranch();
    }
}
