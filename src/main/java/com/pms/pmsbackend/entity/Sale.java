package com.pms.pmsbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(name = "sale_date")
    private LocalDateTime saleDate;

    @Column(name = "total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "payment_method")
    private String paymentMethod;

    // Free-text reference the cashier keys in at checkout for both Cash and
    // Bank payments (e.g. a receipt or transfer reference number).
    @Column(name = "transaction_number", length = 100)
    private String transactionNumber;

    // Only set when paymentMethod is "BANK" -- which registered bank the
    // payment was made through.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private Bank bank;

    @Column(name = "payment_status")
    private String paymentStatus = "PAID";

    @Column(name = "amount_paid")
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "amount_due")
    private BigDecimal amountDue = BigDecimal.ZERO;

    @Column(name = "points_earned")
    private Integer pointsEarned = 0;

    @Column(name = "points_redeemed")
    private Integer pointsRedeemed = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private User cashier;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.saleDate == null) this.saleDate = LocalDateTime.now();
    }
}