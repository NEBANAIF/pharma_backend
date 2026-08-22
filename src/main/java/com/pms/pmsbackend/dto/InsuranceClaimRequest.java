package com.pms.pmsbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InsuranceClaimRequest {

    @NotNull(message = "Sale is required")
    private Integer saleId;

    @NotNull(message = "Insurance provider is required")
    private Integer insuranceProviderId;

    // Optional: overrides the customer's/provider's coverage % for this claim only.
    // If omitted, the service derives claimedAmount from the customer's stored coverage.
    private BigDecimal coveragePctOverride;

    private String notes;
}
