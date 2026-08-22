package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.entity.Branch;
import com.pms.pmsbackend.repository.BranchRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchRepository branchRepository;

    @GetMapping
    public List<Branch> getAll() {
        return branchRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Branch create(@Valid @RequestBody Branch branch) {
        branch.setId(null);
        return branchRepository.save(branch);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Branch update(@PathVariable Integer id, @Valid @RequestBody Branch branch) {
        Branch existing = branchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + id));
        existing.setName(branch.getName());
        existing.setAddress(branch.getAddress());
        existing.setPhone(branch.getPhone());
        return branchRepository.save(existing);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + id));
        if (Boolean.TRUE.equals(branch.getIsMain())) {
            throw new IllegalArgumentException("The main branch cannot be deleted.");
        }
        branchRepository.deleteById(id);
    }
}
