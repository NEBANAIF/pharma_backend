package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TodayCashSummaryDto {
    private BigDecimal cashSales;
    private BigDecimal cashExpenses;
    private boolean alreadyClosed;
    private CashClosingResponse existingClosing; // populated if alreadyClosed
}
