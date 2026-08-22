package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class InsuranceClaimResponse {
    private Integer id;
    private Integer saleId;
    private BigDecimal saleTotalAmount;
    private String customerName;
    private Integer insuranceProviderId;
    private String insuranceProviderName;
    private String policyNumber;
    private BigDecimal claimedAmount;
    private BigDecimal approvedAmount;
    private String status;
    private String notes;
    private String createdByName;
    private String resolvedByName;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime resolvedAt;
}
