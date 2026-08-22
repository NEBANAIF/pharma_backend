package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.ApprovedMedicineRequest;
import com.pms.pmsbackend.dto.ApprovedMedicineResponse;
import com.pms.pmsbackend.service.InsuranceFormularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insurance-providers/{providerId}/approved-medicines")
@RequiredArgsConstructor
public class InsuranceFormularyController {

    private final InsuranceFormularyService formularyService;

    @GetMapping
    public List<ApprovedMedicineResponse> getAll(@PathVariable Integer providerId) {
        return formularyService.findByProvider(providerId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovedMedicineResponse add(@PathVariable Integer providerId, @Valid @RequestBody ApprovedMedicineRequest request) {
        return formularyService.add(providerId, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/{approvedMedicineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Integer providerId, @PathVariable Integer approvedMedicineId) {
        formularyService.remove(providerId, approvedMedicineId);
    }
}
