package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.entity.Supplier;
import com.pms.pmsbackend.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepository supplierRepository;

    @GetMapping
    public List<Supplier> getAll() {
        return supplierRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Supplier create(@Valid @RequestBody Supplier supplier) {
        supplier.setId(null);
        return supplierRepository.save(supplier);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    @PutMapping("/{id}")
    public Supplier update(@PathVariable Integer id, @Valid @RequestBody Supplier supplier) {
        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
        existing.setName(supplier.getName());
        existing.setContactPerson(supplier.getContactPerson());
        existing.setPhone(supplier.getPhone());
        existing.setEmail(supplier.getEmail());
        existing.setAddress(supplier.getAddress());
        return supplierRepository.save(existing);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!supplierRepository.existsById(id)) {
            throw new EntityNotFoundException("Supplier not found: " + id);
        }
        supplierRepository.deleteById(id);
    }
}
