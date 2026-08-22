package com.pms.pmsbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A registered bank a cashier can attribute a "Bank" payment to at
 * checkout (see Sale.bank / Sale.transactionNumber). Kept intentionally
 * simple, mirroring Branch/Category -- no ledger or reconciliation logic,
 * just a name + account number to select from and record against a sale.
 */
@Entity
@Table(name = "banks")
@Getter
@Setter
@NoArgsConstructor
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Bank name is required")
    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "account_number", length = 60)
    private String accountNumber;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
