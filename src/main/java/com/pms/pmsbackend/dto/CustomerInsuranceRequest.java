package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CustomerInsuranceRequest {

    // Null clears the customer's insurance (e.g. policy lapsed).
    private Integer insuranceProviderId;

    private String insurancePolicyNumber;

    // Optional override of the provider's default coverage % for this customer.
    private BigDecimal insuranceCoveragePct;
}
