package com.pms.pmsbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_register_closings")
@Getter
@Setter
@NoArgsConstructor
public class CashRegisterClosing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "closing_date", unique = true, nullable = false)
    private LocalDate closingDate;

    @Column(name = "opening_balance")
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "cash_sales")
    private BigDecimal cashSales = BigDecimal.ZERO;

    @Column(name = "cash_expenses")
    private BigDecimal cashExpenses = BigDecimal.ZERO;

    @Column(name = "expected_cash")
    private BigDecimal expectedCash = BigDecimal.ZERO;

    @Column(name = "actual_cash")
    private BigDecimal actualCash = BigDecimal.ZERO;

    private BigDecimal difference = BigDecimal.ZERO;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
