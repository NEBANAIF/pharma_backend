package com.pms.pmsbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CashClosingRequest {

    @NotNull(message = "Opening balance is required")
    private BigDecimal openingBalance;

    @NotNull(message = "Actual cash counted is required")
    private BigDecimal actualCash;

    private String notes;
}
