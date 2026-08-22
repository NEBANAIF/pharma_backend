package com.pms.pmsbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SaleRequest {

    private Integer customerId; // optional -- null means walk-in customer

    @NotEmpty(message = "Sale must contain at least one item")
    @Valid
    private List<SaleItemRequest> items;

    private BigDecimal discount = BigDecimal.ZERO;

    private String paymentMethod = "CASH"; // CASH, BANK

    // Reference/receipt number keyed in by the cashier -- required for both
    // Cash and Bank payments.
    private String transactionNumber;

    // Which registered bank the payment went through -- required when
    // paymentMethod is "BANK", ignored otherwise.
    private Integer bankId;

    private String paymentStatus = "PAID"; // PAID, PARTIAL, CREDIT

    // How much the customer actually handed over right now. For PAID this is
    // ignored (assumed to equal the total). For PARTIAL it must be > 0 and
    // less than the total. For CREDIT it defaults to 0 (nothing paid yet).
    private BigDecimal amountPaid;

    // Loyalty points the customer wants to redeem against this sale (each
    // point is worth 0.1 currency units). Requires a registered customer.
    private Integer redeemPoints = 0;
}