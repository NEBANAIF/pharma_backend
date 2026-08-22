package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.PurchaseRequest;
import com.pms.pmsbackend.dto.PurchaseResponse;
import com.pms.pmsbackend.dto.ReceivePurchaseRequest;
import com.pms.pmsbackend.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER', 'MANAGER')")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    public List<PurchaseResponse> getAll() {
        return purchaseService.findAll();
    }

    @GetMapping("/{id}")
    public PurchaseResponse getById(@PathVariable Integer id) {
        return purchaseService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse create(@Valid @RequestBody PurchaseRequest request) {
        return purchaseService.create(request);
    }

    @PostMapping("/{id}/receive")
    public PurchaseResponse receive(@PathVariable Integer id, @Valid @RequestBody ReceivePurchaseRequest request) {
        return purchaseService.receive(id, request);
    }

    @PostMapping("/{id}/cancel")
    public PurchaseResponse cancel(@PathVariable Integer id) {
        return purchaseService.cancel(id);
    }
}
