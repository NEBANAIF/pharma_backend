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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final MedicineRepository medicineRepository;
    private final BatchRepository batchRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final BankRepository bankRepository;
    private final StockMovementService stockMovementService;

    // Simple, symmetric loyalty rate: every 10 currency units spent earns 1
    // point, and every point redeemed is worth 0.1 currency units back.
    private static final BigDecimal CURRENCY_PER_POINT = BigDecimal.TEN;

    @Transactional
    public SaleResponse checkout(SaleRequest request) {
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + request.getCustomerId()));
        }

        String paymentStatus = request.getPaymentStatus() != null ? request.getPaymentStatus() : "PAID";
        if (!paymentStatus.equals("PAID") && customer == null) {
            throw new IllegalArgumentException("A customer must be selected for PARTIAL or CREDIT sales.");
        }

        int redeemPoints = request.getRedeemPoints() != null ? request.getRedeemPoints() : 0;
        if (redeemPoints > 0 && customer == null) {
            throw new IllegalArgumentException("A customer must be selected to redeem loyalty points.");
        }
        if (redeemPoints > 0 && customer != null) {
            int available = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
            if (redeemPoints > available) {
                throw new IllegalArgumentException(
                        "Customer only has " + available + " loyalty points available.");
            }
        }

        Sale saved = buildAndSaveSale(
                customer, null, request.getItems(),
                request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO,
                request.getPaymentMethod(), paymentStatus, request.getAmountPaid(), redeemPoints,
                request.getTransactionNumber(), request.getBankId());

        return toResponse(saved, computeSubtotal(saved));
    }

    /**
     * Reused by PrescriptionService when dispensing a verified prescription --
     * runs the same FEFO stock deduction and produces a real Sale, just linked
     * back to the prescription it fulfilled.
     */
    @Transactional
    public Sale checkoutForPrescription(Customer customer, Prescription prescription, List<SaleItemRequest> items) {
        return buildAndSaveSale(customer, prescription, items, BigDecimal.ZERO, "CASH", "PAID", null, 0, null, null);
    }

    private Sale buildAndSaveSale(Customer customer, Prescription prescription, List<SaleItemRequest> items,
                                   BigDecimal discount, String paymentMethod, String paymentStatus,
                                   BigDecimal amountPaidInput, int redeemPoints,
                                   String transactionNumber, Integer bankId) {
        Sale sale = new Sale();
        sale.setCustomer(customer);
        sale.setPrescription(prescription);

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        User cashier = username != null ? userRepository.findByUsername(username).orElse(null) : null;
        if (cashier != null) {
            sale.setCashier(cashier);
        }

        Branch branch = resolveBranch(cashier);
        sale.setBranch(branch);

        sale.setPaymentMethod(paymentMethod);
        sale.setPaymentStatus(paymentStatus);
        sale.setTransactionNumber(transactionNumber);

        if ("BANK".equals(paymentMethod)) {
            if (bankId == null) {
                throw new IllegalArgumentException("Select a bank for a Bank payment.");
            }
            Bank bank = bankRepository.findById(bankId)
                    .orElseThrow(() -> new EntityNotFoundException("Bank not found: " + bankId));
            sale.setBank(bank);
        }

        sale.setDiscount(discount != null ? discount : BigDecimal.ZERO);

        BigDecimal subtotalTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();
        // One stock movement per batch actually touched (not just per medicine
        // line) so Stock History always shows which batch a sale drew from --
        // recorded after the sale itself is saved, once we have a real sale ID
        // to reference. A running "stock so far" counter lets each fragment's
        // before/after reflect its place in the overall deduction.
        List<PendingStockMovement> pendingMovements = new ArrayList<>();

        for (SaleItemRequest itemReq : items) {
            Medicine medicine = medicineRepository.findById(itemReq.getMedicineId())
                    .orElseThrow(() -> new EntityNotFoundException("Medicine not found: " + itemReq.getMedicineId()));

            int remaining = itemReq.getQuantity();
            int available = batchRepository.sumQuantityByMedicineIdAndBranchId(medicine.getId(), branch.getId());
            if (available < remaining) {
                throw new IllegalArgumentException(
                        "Insufficient stock for " + medicine.getName() + " at " + branch.getName() +
                        ": available " + available + ", requested " + remaining);
            }

            List<Batch> batches;
            if (itemReq.getBatchId() != null) {
                Batch requestedBatch = batchRepository.findById(itemReq.getBatchId())
                        .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + itemReq.getBatchId()));
                if (!requestedBatch.getMedicine().getId().equals(medicine.getId())) {
                    throw new IllegalArgumentException(
                            "Batch " + requestedBatch.getBatchNumber() + " does not belong to " + medicine.getName() + ".");
                }
                if (requestedBatch.getBranch() == null || !requestedBatch.getBranch().getId().equals(branch.getId())) {
                    throw new IllegalArgumentException(
                            "Batch " + requestedBatch.getBatchNumber() + " is not stocked at " + branch.getName() + ".");
                }
                if (requestedBatch.getQuantity() < remaining) {
                    throw new IllegalArgumentException(
                            "Insufficient stock in batch " + requestedBatch.getBatchNumber() + " for " + medicine.getName() +
                            ": available " + requestedBatch.getQuantity() + ", requested " + remaining);
                }
                batches = List.of(requestedBatch);
            } else {
                // No batch chosen -- default to FEFO (earliest expiry first) across all batches
                batches = batchRepository.findByMedicineIdAndBranchIdOrderByExpiryDateAsc(medicine.getId(), branch.getId());
            }

            int runningStock = available;

            for (Batch batch : batches) {
                if (remaining <= 0) break;
                if (batch.getQuantity() <= 0) continue;

                int takeFromBatch = Math.min(remaining, batch.getQuantity());
                batch.setQuantity(batch.getQuantity() - takeFromBatch);
                batchRepository.save(batch);

                BigDecimal lineSubtotal = medicine.getSellingPrice().multiply(BigDecimal.valueOf(takeFromBatch));
                BigDecimal lineTax = lineSubtotal
                        .multiply(medicine.getTaxPercent() != null ? medicine.getTaxPercent() : BigDecimal.ZERO)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                SaleItem saleItem = new SaleItem();
                saleItem.setSale(sale);
                saleItem.setMedicine(medicine);
                saleItem.setBatch(batch);
                saleItem.setQuantity(takeFromBatch);
                saleItem.setUnitPrice(medicine.getSellingPrice());
                saleItem.setSubtotal(lineSubtotal);
                saleItems.add(saleItem);

                subtotalTotal = subtotalTotal.add(lineSubtotal);
                taxTotal = taxTotal.add(lineTax);

                int stockAfterThisBatch = runningStock - takeFromBatch;
                pendingMovements.add(new PendingStockMovement(medicine, branch, batch, runningStock, stockAfterThisBatch));
                runningStock = stockAfterThisBatch;

                remaining -= takeFromBatch;
            }
        }

        BigDecimal preRedemptionTotal = subtotalTotal.add(taxTotal).subtract(sale.getDiscount());
        if (preRedemptionTotal.compareTo(BigDecimal.ZERO) < 0) preRedemptionTotal = BigDecimal.ZERO;

        BigDecimal redemptionValue = BigDecimal.ZERO;
        if (redeemPoints > 0) {
            // 1 point = 1/10 currency unit, matching the 10-units-spent-per-point earn rate.
            redemptionValue = BigDecimal.valueOf(redeemPoints).divide(CURRENCY_PER_POINT, 2, RoundingMode.HALF_UP);
            // Never let redemption push the sale below zero -- cap it at the total.
            if (redemptionValue.compareTo(preRedemptionTotal) > 0) {
                redemptionValue = preRedemptionTotal;
            }
        }
        BigDecimal total = preRedemptionTotal.subtract(redemptionValue);

        sale.setItems(saleItems);
        sale.setTaxAmount(taxTotal);
        sale.setTotalAmount(total);

        BigDecimal amountPaid;
        switch (paymentStatus) {
            case "CREDIT":
                amountPaid = BigDecimal.ZERO;
                break;
            case "PARTIAL":
                if (amountPaidInput == null || amountPaidInput.compareTo(BigDecimal.ZERO) <= 0
                        || amountPaidInput.compareTo(total) >= 0) {
                    throw new IllegalArgumentException(
                            "A partial sale needs an amount paid that's greater than 0 and less than the total.");
                }
                amountPaid = amountPaidInput;
                break;
            default: // PAID
                amountPaid = total;
        }
        BigDecimal amountDue = total.subtract(amountPaid);
        sale.setAmountPaid(amountPaid);
        sale.setAmountDue(amountDue);

        // Registered customers earn 1 point per 10 currency units of the final
        // (post-redemption) total, and spend whatever points they redeemed.
        // Anything left unpaid also goes on their tab. Both updates land on
        // the same entity, so save it once at the end.
        if (customer != null) {
            boolean customerDirty = false;

            if (amountDue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentBalance = customer.getCreditBalance() != null ? customer.getCreditBalance() : BigDecimal.ZERO;
                customer.setCreditBalance(currentBalance.add(amountDue));
                customerDirty = true;
            }

            int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
            int earnedPoints = total.divide(CURRENCY_PER_POINT, 0, RoundingMode.DOWN).intValue();
            int updatedPoints = currentPoints - redeemPoints + earnedPoints;
            if (redeemPoints > 0 || earnedPoints > 0) {
                customer.setLoyaltyPoints(Math.max(updatedPoints, 0));
                customerDirty = true;
            }
            sale.setPointsEarned(earnedPoints);
            sale.setPointsRedeemed(redeemPoints);

            if (customerDirty) {
                customerRepository.save(customer);
            }
        }

        Sale saved = saleRepository.save(sale);

        for (PendingStockMovement pm : pendingMovements) {
            stockMovementService.record(pm.medicine(), pm.branch(), pm.batch(), "SALE",
                    pm.before(), pm.after(), "Sale #" + saved.getId(), null);
        }

        return saved;
    }

    // One entry per batch fragment touched during a sale, held until the sale
    // has a real ID to reference the stock movement against.
    private record PendingStockMovement(Medicine medicine, Branch branch, Batch batch, int before, int after) {}

    /** Sales are scoped to the cashier's assigned branch; branch-less users (e.g. Admin) fall back to the Main Branch. */
    private Branch resolveBranch(User user) {
        if (user != null && user.getBranch() != null) {
            return user.getBranch();
        }
        return branchRepository.findByIsMainTrue()
                .orElseThrow(() -> new IllegalStateException("No Main Branch configured -- run the branches migration."));
    }

    public List<SaleResponse> findAll() {
        return saleRepository.findAllByOrderBySaleDateDesc().stream()
                .map(sale -> toResponse(sale, computeSubtotal(sale)))
                .collect(Collectors.toList());
    }

    public List<SaleResponse> findByCustomer(Integer customerId) {
        return saleRepository.findByCustomerIdOrderBySaleDateDesc(customerId).stream()
                .map(sale -> toResponse(sale, computeSubtotal(sale)))
                .collect(Collectors.toList());
    }

    public SaleResponse findById(Integer id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found: " + id));
        return toResponse(sale, computeSubtotal(sale));
    }

    private BigDecimal computeSubtotal(Sale sale) {
        return sale.getItems().stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private SaleResponse toResponse(Sale sale, BigDecimal subtotal) {
        SaleResponse dto = new SaleResponse();
        dto.setId(sale.getId());
        dto.setSaleDate(sale.getSaleDate());
        dto.setCustomerId(sale.getCustomer() != null ? sale.getCustomer().getId() : null);
        dto.setCustomerName(sale.getCustomer() != null ? sale.getCustomer().getName() : "Walk-in");
        dto.setCashierName(sale.getCashier() != null ? sale.getCashier().getFullName() : null);
        if (sale.getBranch() != null) {
            dto.setBranchId(sale.getBranch().getId());
            dto.setBranchName(sale.getBranch().getName());
        }
        dto.setSubtotal(subtotal);
        dto.setDiscount(sale.getDiscount());
        dto.setTaxAmount(sale.getTaxAmount());
        dto.setTotalAmount(sale.getTotalAmount());
        dto.setPaymentMethod(sale.getPaymentMethod());
        dto.setTransactionNumber(sale.getTransactionNumber());
        if (sale.getBank() != null) {
            dto.setBankId(sale.getBank().getId());
            dto.setBankName(sale.getBank().getName());
        }
        dto.setPaymentStatus(sale.getPaymentStatus());
        dto.setAmountPaid(sale.getAmountPaid());
        dto.setAmountDue(sale.getAmountDue());
        dto.setPointsEarned(sale.getPointsEarned());
        dto.setPointsRedeemed(sale.getPointsRedeemed());
        if (sale.getCustomer() != null) {
            dto.setCustomerLoyaltyPoints(sale.getCustomer().getLoyaltyPoints());
        }

        List<SaleItemResponse> items = sale.getItems().stream().map(item -> {
            SaleItemResponse itemDto = new SaleItemResponse();
            itemDto.setId(item.getId());
            itemDto.setMedicineId(item.getMedicine().getId());
            itemDto.setMedicineName(item.getMedicine().getName());
            itemDto.setBatchNumber(item.getBatch() != null ? item.getBatch().getBatchNumber() : null);
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setSubtotal(item.getSubtotal());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }
}