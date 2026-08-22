package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.BatchDto;
import com.pms.pmsbackend.entity.Batch;
import com.pms.pmsbackend.entity.Branch;
import com.pms.pmsbackend.entity.Medicine;
import com.pms.pmsbackend.entity.Supplier;
import com.pms.pmsbackend.repository.BatchRepository;
import com.pms.pmsbackend.repository.BranchRepository;
import com.pms.pmsbackend.repository.MedicineRepository;
import com.pms.pmsbackend.repository.SupplierRepository;
import com.pms.pmsbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final MedicineRepository medicineRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final StockMovementService stockMovementService;

    public List<BatchDto> findByMedicine(Integer medicineId) {
        return batchRepository.findByMedicineIdWithDetailsOrderByExpiryDateAsc(medicineId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BatchDto create(BatchDto dto) {
        Medicine medicine = medicineRepository.findById(dto.getMedicineId())
                .orElseThrow(() -> new EntityNotFoundException("Medicine not found: " + dto.getMedicineId()));

        Branch branch = resolveBranch(dto.getBranchId());
        int before = batchRepository.sumQuantityByMedicineIdAndBranchId(medicine.getId(), branch.getId());

        Batch batch = new Batch();
        batch.setMedicine(medicine);
        batch.setBatchNumber(dto.getBatchNumber());
        batch.setManufactureDate(dto.getManufactureDate());
        batch.setExpiryDate(dto.getExpiryDate());
        batch.setQuantity(dto.getQuantity());
        batch.setBranch(branch);

        if (dto.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + dto.getSupplierId()));
            batch.setSupplier(supplier);
        }

        Batch saved = batchRepository.save(batch);

        int after = before + (dto.getQuantity() != null ? dto.getQuantity() : 0);
        stockMovementService.record(medicine, branch, saved, "PURCHASE",
                before, after, "Batch " + saved.getBatchNumber(), null);

        return toDto(saved);
    }

    public void delete(Integer id) {
        if (!batchRepository.existsById(id)) {
            throw new EntityNotFoundException("Batch not found: " + id);
        }
        batchRepository.deleteById(id);
    }

    /**
     * Resolves which branch a new batch belongs to: explicit branchId if given,
     * otherwise the current user's assigned branch, otherwise the Main Branch.
     */
    private Branch resolveBranch(Integer explicitBranchId) {
        if (explicitBranchId != null) {
            return branchRepository.findById(explicitBranchId)
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + explicitBranchId));
        }

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        if (username != null) {
            var currentUser = userRepository.findByUsername(username).orElse(null);
            if (currentUser != null && currentUser.getBranch() != null) {
                return currentUser.getBranch();
            }
        }

        return branchRepository.findByIsMainTrue()
                .orElseThrow(() -> new IllegalStateException("No branch specified and no Main Branch configured."));
    }

    private BatchDto toDto(Batch batch) {
        BatchDto dto = new BatchDto();
        dto.setId(batch.getId());
        dto.setMedicineId(batch.getMedicine().getId());
        dto.setMedicineName(batch.getMedicine().getName());
        dto.setBatchNumber(batch.getBatchNumber());
        dto.setManufactureDate(batch.getManufactureDate());
        dto.setExpiryDate(batch.getExpiryDate());
        dto.setQuantity(batch.getQuantity());
        if (batch.getSupplier() != null) {
            dto.setSupplierId(batch.getSupplier().getId());
            dto.setSupplierName(batch.getSupplier().getName());
        }
        if (batch.getBranch() != null) {
            dto.setBranchId(batch.getBranch().getId());
            dto.setBranchName(batch.getBranch().getName());
        }
        return dto;
    }
}
