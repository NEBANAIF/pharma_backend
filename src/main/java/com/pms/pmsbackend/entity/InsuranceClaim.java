package com.pms.pmsbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_claims")
@Getter
@Setter
@NoArgsConstructor
public class InsuranceClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_provider_id", nullable = false)
    private InsuranceProvider insuranceProvider;

    // Snapshot of the customer's policy number at claim time, so it stays
    // accurate even if the customer's record changes later.
    @Column(name = "policy_number")
    private String policyNumber;

    @Column(name = "claimed_amount", nullable = false)
    private BigDecimal claimedAmount;

    @Column(name = "approved_amount")
    private BigDecimal approvedAmount;

    // PENDING -> SUBMITTED -> APPROVED | REJECTED -> PAID
    @Column(nullable = false)
    private String status = "PENDING";

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
