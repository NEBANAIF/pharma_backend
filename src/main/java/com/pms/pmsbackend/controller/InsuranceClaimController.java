package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.InsuranceClaimRequest;
import com.pms.pmsbackend.dto.InsuranceClaimResponse;
import com.pms.pmsbackend.dto.ResolveClaimRequest;
import com.pms.pmsbackend.service.InsuranceClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insurance-claims")
@RequiredArgsConstructor
public class InsuranceClaimController {

    private final InsuranceClaimService insuranceClaimService;

    @GetMapping
    public List<InsuranceClaimResponse> getAll() {
        return insuranceClaimService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'PHARMACIST', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InsuranceClaimResponse create(@Valid @RequestBody InsuranceClaimRequest request) {
        return insuranceClaimService.create(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'PHARMACIST', 'MANAGER')")
    @PostMapping("/{id}/submit")
    public InsuranceClaimResponse submit(@PathVariable Integer id) {
        return insuranceClaimService.submit(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/{id}/resolve")
    public InsuranceClaimResponse resolve(@PathVariable Integer id, @Valid @RequestBody ResolveClaimRequest request) {
        return insuranceClaimService.resolve(id, request);
    }
}
