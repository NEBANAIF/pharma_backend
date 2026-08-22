package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.CashClosingRequest;
import com.pms.pmsbackend.dto.CashClosingResponse;
import com.pms.pmsbackend.dto.TodayCashSummaryDto;
import com.pms.pmsbackend.service.CashRegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cash-register")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    @GetMapping("/today")
    public TodayCashSummaryDto getTodaySummary() {
        return cashRegisterService.getTodaySummary();
    }

    @PostMapping("/close")
    public CashClosingResponse closeToday(@Valid @RequestBody CashClosingRequest request) {
        return cashRegisterService.closeToday(request);
    }

    @GetMapping
    public List<CashClosingResponse> getAll() {
        return cashRegisterService.findAll();
    }
}
