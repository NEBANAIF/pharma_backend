package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.*;
import com.pms.pmsbackend.entity.*;
import com.pms.pmsbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final MedicineRepository medicineRepository;
    private final SupplierRepository supplierRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final StockMovementService stockMovementService;

    @Transactional
    public PurchaseResponse create(PurchaseRequest request) {
        Purchase purchase = new Purchase();

        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + request.getSupplierId()));
            purchase.setSupplier(supplier);
        }
        purchase.setInvoiceNumber(request.getInvoiceNumber());
        purchase.setStatus("PENDING");

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        User currentUser = username != null ? userRepository.findByUsername(username).orElse(null) : null;
        if (currentUser != null) {
            purchase.setCreatedBy(currentUser);
        }
        purchase.setBranch(resolveBranch(request.getBranchId(), currentUser));

        List<PurchaseItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (PurchaseItemRequest itemReq : request.getItems()) {
            Medicine medicine = medicineRepository.findById(itemReq.getMedicineId())
                    .orElseThrow(() -> new EntityNotFoundException("Medicine not found: " + itemReq.getMedicineId()));

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setMedicine(medicine);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            BigDecimal subtotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            item.setSubtotal(subtotal);
            items.add(item);
            total = total.add(subtotal);
        }

        purchase.setItems(items);
        purchase.setTotalAmount(total);

        Purchase saved = purchaseRepository.save(purchase);
        return toResponse(saved);
    }

    @Transactional
    public PurchaseResponse receive(Integer purchaseId, ReceivePurchaseRequest request) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase not found: " + purchaseId));

        if (!"PENDING".equals(purchase.getStatus())) {
            throw new IllegalArgumentException("Only PENDING purchases can be received. Current status: " + purchase.getStatus());
        }

        Map<Integer, PurchaseItem> itemsById = purchase.getItems().stream()
                .collect(Collectors.toMap(PurchaseItem::getId, i -> i));

        for (ReceiveItemRequest receiveReq : request.getItems()) {
            PurchaseItem item = itemsById.get(receiveReq.getPurchaseItemId());
            if (item == null) {
                throw new EntityNotFoundException(
                        "Purchase item " + receiveReq.getPurchaseItemId() + " does not belong to purchase " + purchaseId);
            }

            Batch batch = new Batch();
            batch.setMedicine(item.getMedicine());
            batch.setBranch(purchase.getBranch());
            batch.setBatchNumber(receiveReq.getBatchNumber());
            batch.setManufactureDate(receiveReq.getManufactureDate());
            batch.setExpiryDate(receiveReq.getExpiryDate());
            batch.setQuantity(item.getQuantity());
            batch.setSupplier(purchase.getSupplier());

            int before = batchRepository.sumQuantityByMedicineIdAndBranchId(item.getMedicine().getId(), purchase.getBranch().getId());
            Batch savedBatch = batchRepository.save(batch);
            item.setBatch(savedBatch);
            purchaseItemRepository.save(item);

            int after = before + item.getQuantity();
            stockMovementService.record(item.getMedicine(), purchase.getBranch(), savedBatch, "PURCHASE",
                    before, after, "Purchase #" + purchase.getId(), null);
        }

        purchase.setStatus("RECEIVED");
        Purchase saved = purchaseRepository.save(purchase);
        return toResponse(saved);
    }

    @Transactional
    public PurchaseResponse cancel(Integer purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase not found: " + purchaseId));

        if (!"PENDING".equals(purchase.getStatus())) {
            throw new IllegalArgumentException("Only PENDING purchases can be cancelled. Current status: " + purchase.getStatus());
        }

        purchase.setStatus("CANCELLED");
        Purchase saved = purchaseRepository.save(purchase);
        return toResponse(saved);
    }

    public List<PurchaseResponse> findAll() {
        return purchaseRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PurchaseResponse findById(Integer id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Purchase not found: " + id));
        return toResponse(purchase);
    }

    /** Explicit branchId wins; otherwise the current user's assigned branch; otherwise the Main Branch. */
    private Branch resolveBranch(Integer explicitBranchId, User currentUser) {
        if (explicitBranchId != null) {
            return branchRepository.findById(explicitBranchId)
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + explicitBranchId));
        }
        if (currentUser != null && currentUser.getBranch() != null) {
            return currentUser.getBranch();
        }
        return branchRepository.findByIsMainTrue()
                .orElseThrow(() -> new IllegalStateException("No Main Branch configured -- run the branches migration."));
    }

    private PurchaseResponse toResponse(Purchase purchase) {
        PurchaseResponse dto = new PurchaseResponse();
        dto.setId(purchase.getId());
        dto.setSupplierId(purchase.getSupplier() != null ? purchase.getSupplier().getId() : null);
        dto.setSupplierName(purchase.getSupplier() != null ? purchase.getSupplier().getName() : "—");
        dto.setInvoiceNumber(purchase.getInvoiceNumber());
        dto.setPurchaseDate(purchase.getPurchaseDate());
        dto.setTotalAmount(purchase.getTotalAmount());
        dto.setStatus(purchase.getStatus());
        dto.setCreatedByName(purchase.getCreatedBy() != null ? purchase.getCreatedBy().getFullName() : null);
        dto.setCreatedAt(purchase.getCreatedAt());
        if (purchase.getBranch() != null) {
            dto.setBranchId(purchase.getBranch().getId());
            dto.setBranchName(purchase.getBranch().getName());
        }

        List<PurchaseItemResponse> items = purchase.getItems().stream().map(item -> {
            PurchaseItemResponse itemDto = new PurchaseItemResponse();
            itemDto.setId(item.getId());
            itemDto.setMedicineId(item.getMedicine().getId());
            itemDto.setMedicineName(item.getMedicine().getName());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setSubtotal(item.getSubtotal());
            if (item.getBatch() != null) {
                itemDto.setBatchId(item.getBatch().getId());
                itemDto.setBatchNumber(item.getBatch().getBatchNumber());
            }
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }
}
