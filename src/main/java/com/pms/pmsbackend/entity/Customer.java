package com.pms.pmsbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false, length = 150)
    private String name;

    private String phone;
    private String email;
    private String address;

    @Column(name = "loyalty_points")
    private Integer loyaltyPoints = 0;

    @Column(name = "credit_balance")
    private BigDecimal creditBalance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_provider_id")
    private InsuranceProvider insuranceProvider;

    @Column(name = "insurance_policy_number")
    private String insurancePolicyNumber;

    // Overrides the provider's default coverage % for this customer when set.
    @Column(name = "insurance_coverage_pct")
    private BigDecimal insuranceCoveragePct;

    // Patient-profile fields. This pharmacy treats each Customer as the
    // patient (Prescriptions and Sales both key off customer_id), so these
    // live here rather than on a separate Patient entity.
    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    private String gender;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "medical_notes", columnDefinition = "TEXT")
    private String medicalNotes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}