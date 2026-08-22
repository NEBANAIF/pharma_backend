package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.StockTransferRequest;
import com.pms.pmsbackend.dto.StockTransferResponse;
import com.pms.pmsbackend.entity.*;
import com.pms.pmsbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final MedicineRepository medicineRepository;
    private final BranchRepository branchRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final StockMovementService stockMovementService;

    @Transactional
    public StockTransferResponse create(StockTransferRequest request) {
        if (request.getFromBranchId().equals(request.getToBranchId())) {
            throw new IllegalArgumentException("Source and destination branch must be different.");
        }

        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> new EntityNotFoundException("Medicine not found: " + request.getMedicineId()));
        Branch fromBranch = getBranch(request.getFromBranchId());
        Branch toBranch = getBranch(request.getToBranchId());

        int available = batchRepository.sumQuantityByMedicineIdAndBranchId(medicine.getId(), fromBranch.getId());
        if (available < request.getQuantity()) {
            throw new IllegalArgumentException(
                    "Insufficient stock at " + fromBranch.getName() + ": available " + available +
                    ", requested " + request.getQuantity());
        }

        StockTransfer transfer = new StockTransfer();
        transfer.setMedicine(medicine);
        transfer.setFromBranch(fromBranch);
        transfer.setToBranch(toBranch);
        transfer.setQuantity(request.getQuantity());
        transfer.setNotes(request.getNotes());
        transfer.setStatus("PENDING");
        currentUser().ifPresent(transfer::setCreatedBy);

        StockTransfer saved = stockTransferRepository.save(transfer);
        return toResponse(saved);
    }

    @Transactional
    public StockTransferResponse complete(Integer id) {
        StockTransfer transfer = getOrThrow(id);
        if (!"PENDING".equals(transfer.getStatus())) {
            throw new IllegalArgumentException("Only PENDING transfers can be completed. Current status: " + transfer.getStatus());
        }

        Integer medicineId = transfer.getMedicine().getId();
        Integer fromBranchId = transfer.getFromBranch().getId();

        // Re-validate at completion time -- stock may have moved since the transfer was requested
        int available = batchRepository.sumQuantityByMedicineIdAndBranchId(medicineId, fromBranchId);
        if (available < transfer.getQuantity()) {
            throw new IllegalArgumentException(
                    "Insufficient stock at " + transfer.getFromBranch().getName() + " to complete this transfer: " +
                    "available " + available + ", need " + transfer.getQuantity());
        }

        int toBranchBefore = batchRepository.sumQuantityByMedicineIdAndBranchId(medicineId, transfer.getToBranch().getId());

        List<Batch> sourceBatches = batchRepository.findByMedicineIdAndBranchIdOrderByExpiryDateAsc(medicineId, fromBranchId);
        int remaining = transfer.getQuantity();

        for (Batch sourceBatch : sourceBatches) {
            if (remaining <= 0) break;
            if (sourceBatch.getQuantity() <= 0) continue;

            int takeFromBatch = Math.min(remaining, sourceBatch.getQuantity());
            sourceBatch.setQuantity(sourceBatch.getQuantity() - takeFromBatch);
            batchRepository.save(sourceBatch);

            // Recreate the same batch (number + expiry) at the destination branch,
            // preserving expiry tracking across the move.
            Batch destBatch = new Batch();
            destBatch.setMedicine(sourceBatch.getMedicine());
            destBatch.setBranch(transfer.getToBranch());
            destBatch.setBatchNumber(sourceBatch.getBatchNumber());
            destBatch.setManufactureDate(sourceBatch.getManufactureDate());
            destBatch.setExpiryDate(sourceBatch.getExpiryDate());
            destBatch.setQuantity(takeFromBatch);
            destBatch.setSupplier(sourceBatch.getSupplier());
            batchRepository.save(destBatch);

            remaining -= takeFromBatch;
        }

        transfer.setStatus("COMPLETED");
        transfer.setCompletedAt(LocalDateTime.now());
        currentUser().ifPresent(transfer::setCompletedBy);

        StockTransferResponse response = toResponse(stockTransferRepository.save(transfer));

        stockMovementService.record(transfer.getMedicine(), transfer.getFromBranch(), null, "TRANSFER_OUT",
                available, available - transfer.getQuantity(), "Transfer #" + transfer.getId(), transfer.getNotes());
        stockMovementService.record(transfer.getMedicine(), transfer.getToBranch(), null, "TRANSFER_IN",
                toBranchBefore, toBranchBefore + transfer.getQuantity(), "Transfer #" + transfer.getId(), transfer.getNotes());

        return response;
    }

    @Transactional
    public StockTransferResponse cancel(Integer id) {
        StockTransfer transfer = getOrThrow(id);
        if (!"PENDING".equals(transfer.getStatus())) {
            throw new IllegalArgumentException("Only PENDING transfers can be cancelled. Current status: " + transfer.getStatus());
        }
        transfer.setStatus("CANCELLED");
        return toResponse(stockTransferRepository.save(transfer));
    }

    public List<StockTransferResponse> findAll() {
        return stockTransferRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Branch getBranch(Integer id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + id));
    }

    private StockTransfer getOrThrow(Integer id) {
        return stockTransferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stock transfer not found: " + id));
    }

    private Optional<User> currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        return username != null ? userRepository.findByUsername(username) : Optional.empty();
    }

    private StockTransferResponse toResponse(StockTransfer t) {
        StockTransferResponse dto = new StockTransferResponse();
        dto.setId(t.getId());
        dto.setMedicineId(t.getMedicine().getId());
        dto.setMedicineName(t.getMedicine().getName());
        dto.setFromBranchId(t.getFromBranch().getId());
        dto.setFromBranchName(t.getFromBranch().getName());
        dto.setToBranchId(t.getToBranch().getId());
        dto.setToBranchName(t.getToBranch().getName());
        dto.setQuantity(t.getQuantity());
        dto.setStatus(t.getStatus());
        dto.setNotes(t.getNotes());
        dto.setCreatedByName(t.getCreatedBy() != null ? t.getCreatedBy().getFullName() : null);
        dto.setCompletedByName(t.getCompletedBy() != null ? t.getCompletedBy().getFullName() : null);
        dto.setCreatedAt(t.getCreatedAt());
        dto.setCompletedAt(t.getCompletedAt());
        return dto;
    }
}
