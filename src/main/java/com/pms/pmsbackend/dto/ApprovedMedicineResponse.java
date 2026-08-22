package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ApprovedMedicineResponse {
    private Integer id;
    private Integer medicineId;
    private String medicineName;
    private BigDecimal coveragePctOverride;
    private BigDecimal effectiveCoveragePct; // override if set, otherwise the provider's default
}
