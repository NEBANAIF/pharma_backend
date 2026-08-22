package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ProfitLossResponse {
    private LocalDate from;
    private LocalDate to;
    private BigDecimal revenue;       // sum of sale item subtotals (pre-tax, pre-discount)
    private BigDecimal costOfGoodsSold;
    private BigDecimal grossProfit;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;
}
