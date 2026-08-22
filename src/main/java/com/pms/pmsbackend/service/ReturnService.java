package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.ReturnRequest;
import com.pms.pmsbackend.dto.ReturnResponse;
import com.pms.pmsbackend.entity.Batch;
import com.pms.pmsbackend.entity.SaleItem;
import com.pms.pmsbackend.entity.SaleReturn;
import com.pms.pmsbackend.repository.BatchRepository;
import com.pms.pmsbackend.repository.SaleItemRepository;
import com.pms.pmsbackend.repository.SaleReturnRepository;
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
public class ReturnService {

    private final SaleReturnRepository saleReturnRepository;
    private final SaleItemRepository saleItemRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final StockMovementService stockMovementService;

    @Transactional
    public ReturnResponse create(ReturnRequest request) {
        SaleItem saleItem = saleItemRepository.findById(request.getSaleItemId())
                .orElseThrow(() -> new EntityNotFoundException("Sale item not found: " + request.getSaleItemId()));

        int alreadyReturned = saleReturnRepository.sumQuantityBySaleItemId(saleItem.getId());
        int remainingReturnable = saleItem.getQuantity() - alreadyReturned;

        if (request.getQuantity() > remainingReturnable) {
            throw new IllegalArgumentException(
                    "Cannot return " + request.getQuantity() + " -- only " + remainingReturnable +
                    " of this line item can still be returned (sold " + saleItem.getQuantity() +
                    ", already returned " + alreadyReturned + ").");
        }

        // Restock: add the returned quantity back to the batch it originally came from, if that batch still exists
        Batch batch = saleItem.getBatch();
        if (batch != null) {
            Integer medicineId = saleItem.getMedicine().getId();
            Integer branchId = batch.getBranch() != null ? batch.getBranch().getId() : null;
            int before = branchId != null
                    ? batchRepository.sumQuantityByMedicineIdAndBranchId(medicineId, branchId)
                    : batchRepository.sumQuantityByMedicineId(medicineId);

            batch.setQuantity(batch.getQuantity() + request.getQuantity());
            batchRepository.save(batch);

            int after = before + request.getQuantity();
            stockMovementService.record(saleItem.getMedicine(), batch.getBranch(), batch, "RETURN",
                    before, after, "Return of Sale #" + saleItem.getSale().getId(), request.getReason());
        }

        SaleReturn saleReturn = new SaleReturn();
        saleReturn.setSaleItem(saleItem);
        saleReturn.setQuantity(request.getQuantity());
        saleReturn.setReason(request.getReason());
        saleReturn.setRefundAmount(saleItem.getUnitPrice().multiply(java.math.BigDecimal.valueOf(request.getQuantity())));

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        if (username != null) {
            userRepository.findByUsername(username).ifPresent(saleReturn::setCreatedBy);
        }

        SaleReturn saved = saleReturnRepository.save(saleReturn);
        return toResponse(saved);
    }

    public List<ReturnResponse> findAll() {
        return saleReturnRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ReturnResponse toResponse(SaleReturn saleReturn) {
        ReturnResponse dto = new ReturnResponse();
        dto.setId(saleReturn.getId());
        dto.setSaleItemId(saleReturn.getSaleItem().getId());
        dto.setSaleId(saleReturn.getSaleItem().getSale().getId());
        dto.setMedicineName(saleReturn.getSaleItem().getMedicine().getName());
        dto.setQuantity(saleReturn.getQuantity());
        dto.setReason(saleReturn.getReason());
        dto.setRefundAmount(saleReturn.getRefundAmount());
        dto.setProcessedByName(saleReturn.getCreatedBy() != null ? saleReturn.getCreatedBy().getFullName() : null);
        dto.setCreatedAt(saleReturn.getCreatedAt());
        return dto;
    }
}
