package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.MedicineDto;
import com.pms.pmsbackend.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping
    public List<MedicineDto> getAll() {
        return medicineService.findAll();
    }

    @GetMapping("/{id}")
    public MedicineDto getById(@PathVariable Integer id) {
        return medicineService.findById(id);
    }

    @GetMapping("/barcode/{barcode}")
    public MedicineDto getByBarcode(@PathVariable String barcode) {
        return medicineService.findByBarcode(barcode);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicineDto create(@Valid @RequestBody MedicineDto dto) {
        return medicineService.create(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    @PutMapping("/{id}")
    public MedicineDto update(@PathVariable Integer id, @Valid @RequestBody MedicineDto dto) {
        return medicineService.update(id, dto);
    }

    // Assigns a fresh barcode to a medicine that doesn't have one (e.g. legacy/imported records),
    // so it can be selected on the label printing page and scanned at POS.
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    @PostMapping("/{id}/regenerate-barcode")
    public MedicineDto regenerateBarcode(@PathVariable Integer id) {
        return medicineService.regenerateBarcode(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        medicineService.delete(id);
    }
}
