package com.pms.pmsbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ApprovedMedicineRequest {

    @NotNull(message = "Medicine is required")
    private Integer medicineId;

    // Optional -- if omitted, the provider's default coverage percentage applies
    private BigDecimal coveragePctOverride;
}
