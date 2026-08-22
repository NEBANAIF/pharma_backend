package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.entity.InsuranceProvider;
import com.pms.pmsbackend.repository.InsuranceProviderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insurance-providers")
@RequiredArgsConstructor
public class InsuranceProviderController {

    private final InsuranceProviderRepository insuranceProviderRepository;

    @GetMapping
    public List<InsuranceProvider> getAll() {
        return insuranceProviderRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InsuranceProvider create(@Valid @RequestBody InsuranceProvider provider) {
        provider.setId(null);
        return insuranceProviderRepository.save(provider);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/{id}")
    public InsuranceProvider update(@PathVariable Integer id, @Valid @RequestBody InsuranceProvider provider) {
        InsuranceProvider existing = insuranceProviderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Insurance provider not found: " + id));
        existing.setName(provider.getName());
        existing.setContactPhone(provider.getContactPhone());
        existing.setContactEmail(provider.getContactEmail());
        existing.setDefaultCoveragePct(provider.getDefaultCoveragePct());
        existing.setIsActive(provider.getIsActive());
        existing.setNotes(provider.getNotes());
        return insuranceProviderRepository.save(existing);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!insuranceProviderRepository.existsById(id)) {
            throw new EntityNotFoundException("Insurance provider not found: " + id);
        }
        insuranceProviderRepository.deleteById(id);
    }
}
