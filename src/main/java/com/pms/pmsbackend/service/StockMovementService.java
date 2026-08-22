package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.StockMovementResponse;
import com.pms.pmsbackend.entity.*;
import com.pms.pmsbackend.repository.StockMovementRepository;
import com.pms.pmsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Central write point for the stock audit trail. Every service that changes
 * a medicine's stock (sales, returns, batch receiving, transfers, discards,
 * manual adjustments) calls {@link #record} right after the change, so the
 * Stock History page has one consistent, complete feed instead of each page
 * reconstructing history from scattered tables.
 */
@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;

    public StockMovement record(Medicine medicine, Branch branch, Batch batch, String source,
                                 int quantityBefore, int quantityAfter, String reference, String notes) {
        StockMovement movement = new StockMovement();
        movement.setMedicine(medicine);
        movement.setBranch(branch);
        movement.setBatch(batch);
        movement.setSource(source);
        movement.setQuantityBefore(quantityBefore);
        movement.setQuantityAfter(quantityAfter);
        movement.setChange(quantityAfter - quantityBefore);
        movement.setReference(reference);
        movement.setNotes(notes);
        currentUser().ifPresent(movement::setPerformedBy);
        return stockMovementRepository.save(movement);
    }

    public List<StockMovementResponse> findAll() {
        return stockMovementRepository.findAllWithDetailsOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<StockMovementResponse> findByMedicine(Integer medicineId) {
        return stockMovementRepository.findByMedicineIdWithDetailsOrderByCreatedAtDesc(medicineId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Optional<User> currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        return username != null ? userRepository.findByUsername(username) : Optional.empty();
    }

    public StockMovementResponse toResponse(StockMovement m) {
        StockMovementResponse dto = new StockMovementResponse();
        dto.setId(m.getId());
        dto.setMedicineId(m.getMedicine().getId());
        dto.setMedicineName(m.getMedicine().getName());
        if (m.getBranch() != null) {
            dto.setBranchId(m.getBranch().getId());
            dto.setBranchName(m.getBranch().getName());
        }
        if (m.getBatch() != null) {
            dto.setBatchNumber(m.getBatch().getBatchNumber());
        }
        dto.setSource(m.getSource());
        dto.setQuantityBefore(m.getQuantityBefore());
        dto.setQuantityAfter(m.getQuantityAfter());
        dto.setChange(m.getChange());
        dto.setReference(m.getReference());
        dto.setNotes(m.getNotes());
        dto.setPerformedByName(m.getPerformedBy() != null ? m.getPerformedBy().getFullName() : null);
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }
}
