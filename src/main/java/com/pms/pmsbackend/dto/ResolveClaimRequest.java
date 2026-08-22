package com.pms.pmsbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ResolveClaimRequest {

    @NotNull(message = "Status is required")
    @Pattern(regexp = "APPROVED|REJECTED|PAID", message = "Status must be APPROVED, REJECTED, or PAID")
    private String status;

    // Required when approving or marking paid; ignored for REJECTED.
    private BigDecimal approvedAmount;

    private String notes;
}
