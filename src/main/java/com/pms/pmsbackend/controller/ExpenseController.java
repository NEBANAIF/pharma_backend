package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.ExpenseResponse;
import com.pms.pmsbackend.entity.Expense;
import com.pms.pmsbackend.entity.User;
import com.pms.pmsbackend.repository.ExpenseRepository;
import com.pms.pmsbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @GetMapping
    public List<ExpenseResponse> getAll() {
        return expenseRepository.findAllByOrderByExpenseDateDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody Expense expense) {
        expense.setId(null);
        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        if (username != null) {
            userRepository.findByUsername(username).ifPresent(expense::setCreatedBy);
        }
        return toResponse(expenseRepository.save(expense));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!expenseRepository.existsById(id)) {
            throw new EntityNotFoundException("Expense not found: " + id);
        }
        expenseRepository.deleteById(id);
    }

    // Maps the entity to a plain response object so we never hand Jackson a
    // lazy Hibernate proxy (e.g. Expense.createdBy.branch) to serialize.
    private ExpenseResponse toResponse(Expense expense) {
        ExpenseResponse response = new ExpenseResponse();
        response.setId(expense.getId());
        response.setCategory(expense.getCategory());
        response.setDescription(expense.getDescription());
        response.setAmount(expense.getAmount());
        response.setExpenseDate(expense.getExpenseDate());
        response.setCreatedAt(expense.getCreatedAt());

        User createdBy = expense.getCreatedBy();
        if (createdBy != null) {
            response.setCreatedById(createdBy.getId());
            response.setCreatedByName(createdBy.getFullName() != null ? createdBy.getFullName() : createdBy.getUsername());
        }
        return response;
    }
}

