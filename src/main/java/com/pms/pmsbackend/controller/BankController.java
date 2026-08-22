package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.entity.Bank;
import com.pms.pmsbackend.repository.BankRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Banks are readable by any authenticated user (a cashier needs the full
 * list to pick from at POS checkout), but only Admin/Manager can register
 * or edit one -- same split used for financial data elsewhere in the app.
 */
@RestController
@RequestMapping("/api/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankRepository bankRepository;

    @GetMapping
    public List<Bank> getAll() {
        return bankRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Bank create(@Valid @RequestBody Bank bank) {
        bank.setId(null);
        return bankRepository.save(bank);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/{id}")
    public Bank update(@PathVariable Integer id, @Valid @RequestBody Bank bank) {
        Bank existing = bankRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bank not found: " + id));
        existing.setName(bank.getName());
        existing.setAccountNumber(bank.getAccountNumber());
        return bankRepository.save(existing);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!bankRepository.existsById(id)) {
            throw new EntityNotFoundException("Bank not found: " + id);
        }
        bankRepository.deleteById(id);
    }
}
