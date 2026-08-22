package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.SaleRequest;
import com.pms.pmsbackend.dto.SaleResponse;
import com.pms.pmsbackend.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public List<SaleResponse> getAll() {
        return saleService.findAll();
    }

    @GetMapping("/{id}")
    public SaleResponse getById(@PathVariable Integer id) {
        return saleService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'PHARMACIST', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse checkout(@Valid @RequestBody SaleRequest request) {
        return saleService.checkout(request);
    }
}
