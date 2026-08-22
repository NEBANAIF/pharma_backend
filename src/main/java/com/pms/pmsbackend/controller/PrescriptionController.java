package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.PrescriptionRequest;
import com.pms.pmsbackend.dto.PrescriptionResponse;
import com.pms.pmsbackend.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER', 'MANAGER')")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping
    public List<PrescriptionResponse> getAll() {
        return prescriptionService.findAll();
    }

    @GetMapping("/{id}")
    public PrescriptionResponse getById(@PathVariable Integer id) {
        return prescriptionService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionResponse create(@Valid @RequestBody PrescriptionRequest request) {
        return prescriptionService.create(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    @PostMapping("/{id}/verify")
    public PrescriptionResponse verify(@PathVariable Integer id) {
        return prescriptionService.verify(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    @PostMapping("/{id}/dispense")
    public PrescriptionResponse dispense(@PathVariable Integer id) {
        return prescriptionService.dispense(id);
    }

    @PostMapping("/{id}/cancel")
    public PrescriptionResponse cancel(@PathVariable Integer id) {
        return prescriptionService.cancel(id);
    }
}
