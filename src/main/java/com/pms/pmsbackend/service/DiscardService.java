package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.DiscardRequest;
import com.pms.pmsbackend.dto.StockMovementResponse;
import com.pms.pmsbackend.entity.Batch;
import com.pms.pmsbackend.entity.Branch;
import com.pms.pmsbackend.entity.Medicine;
import com.pms.pmsbackend.entity.User;
import com.pms.pmsbackend.repository.BatchRepository;
import com.pms.pmsbackend.repository.BranchRepository;
import com.pms.pmsbackend.repository.MedicineRepository;
import com.pms.pmsbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DiscardService {

    private static final Set<String> VALID_REASONS = Set.of("EXPIRED", "DAMAGED", "OTHER");

    private final MedicineRepository medicineRepository;
    private final BranchRepository branchRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final StockMovementService stockMovementService;

    @Transactional
    public StockMovementResponse create(DiscardRequest request) {
        if (!VALID_REASONS.contains(request.getReason())) {
            throw new IllegalArgumentException("Reason must be one of EXPIRED, DAMAGED, OTHER.");
        }

        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> new EntityNotFoundException("Medicine not found: " + request.getMedicineId()));
        Branch branch = resolveBranch(request.getBranchId());

        int before = batchRepository.sumQuantityByMedicineIdAndBranchId(medicine.getId(), branch.getId());
        if (before < request.getQuantity()) {
            throw new IllegalArgumentException(
                    "Cannot discard " + request.getQuantity() + " -- only " + before +
                    " of " + medicine.getName() + " available at " + branch.getName() + ".");
        }

        // FEFO: discard from the soonest-expiring batches first, same as a sale would consume them.
        List<Batch> batches = batchRepository.findByMedicineIdAndBranchIdOrderByExpiryDateAsc(medicine.getId(), branch.getId());
        int remaining = request.getQuantity();
        Batch lastTouchedBatch = null;

        for (Batch batch : batches) {
            if (remaining <= 0) break;
            if (batch.getQuantity() <= 0) continue;

            int takeFromBatch = Math.min(remaining, batch.getQuantity());
            batch.setQuantity(batch.getQuantity() - takeFromBatch);
            batchRepository.save(batch);
            lastTouchedBatch = batch;

            remaining -= takeFromBatch;
        }

        int after = before - request.getQuantity();
        String reference = "Discard: " + request.getReason();
        return stockMovementService.toResponse(stockMovementService.record(
                medicine, branch, lastTouchedBatch, "DISCARD", before, after, reference, request.getNotes()));
    }

    /** Same fallback used everywhere else stock is touched without an explicit branch: current user's branch, else Main Branch. */
    private Branch resolveBranch(Integer explicitBranchId) {
        if (explicitBranchId != null) {
            return branchRepository.findById(explicitBranchId)
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + explicitBranchId));
        }

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        if (username != null) {
            User currentUser = userRepository.findByUsername(username).orElse(null);
            if (currentUser != null && currentUser.getBranch() != null) {
                return currentUser.getBranch();
            }
        }

        return branchRepository.findByIsMainTrue()
                .orElseThrow(() -> new IllegalStateException("No branch specified and no Main Branch configured."));
    }
}
