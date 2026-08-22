package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CashClosingResponse {
    private Integer id;
    private LocalDate closingDate;
    private BigDecimal openingBalance;
    private BigDecimal cashSales;
    private BigDecimal cashExpenses;
    private BigDecimal expectedCash;
    private BigDecimal actualCash;
    private BigDecimal difference;
    private String notes;
    private String closedByName;
}
